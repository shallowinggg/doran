package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.*;
import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.RetryCountExhaustedException;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation for rabbitmq producer
 *
 * @author shallowinggg
 */
public class RabbitMQProducer extends AbstractBuiltInProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQProducer.class);
    private final String name;
    private final Channel channel;
    private final String exchangeName;
    private final String routingKey;

    /**
     * Cache for resending unconfirmed messages
     */
    private final ResendCache resendCache = new ResendCache();

    /**
     * Retry tool for network problems when send message.
     * Retry for at most two times, then it will be resent
     * in the future, this can decrease time cost for one
     * message.
     */
    private final Retryer<Void> messageRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(2))
            .build();

    public RabbitMQProducer(String name, RabbitMQConfig config) {
        Assert.hasText(name, "'name' must has text");
        Assert.notNull(config, "'config' must not be null");

        Connection connection = ConnectionFactoryCache.getInstance().getRabbitMQConnection(config);
        Retryer<Channel> retryer = RetryerBuilder.<Channel>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        Channel channel = null;
        try {
            channel = retryer.call(() -> {
                Channel innerChannel = null;
                try {
                    innerChannel = connection.createChannel();
                    innerChannel.confirmSelect();
                    innerChannel.addConfirmListener(new ConfirmListener() {
                        @Override
                        public void handleAck(long deliveryTag, boolean multiple) {
                            if (!multiple) {
                                // TODO: think about using executor() handle it
                                resendCache.delete(deliveryTag);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Message {} send success", deliveryTag);
                                }
                            } else {
                                resendCache.delete(deliveryTag, true);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Message {} and earlier send success", deliveryTag);
                                }
                            }
                        }

                        @Override
                        public void handleNack(long deliveryTag, boolean multiple) {
                            if (!multiple) {
                                if (LOGGER.isWarnEnabled()) {
                                    LOGGER.warn("Message {} is nack, wait for resend", deliveryTag);
                                }
                            } else {
                                if (LOGGER.isWarnEnabled()) {
                                    LOGGER.warn("Message {} and earlier are nack, wait for resend", deliveryTag);
                                }
                            }
                        }
                    });
                    innerChannel.addReturnListener(r -> {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Message can't be routed, exchange: {}, routingKey: {}",
                                    r.getExchange(), r.getRoutingKey());
                        }
                    });
                    return innerChannel;
                } catch (IOException e) {
                    if (innerChannel != null) {
                        innerChannel.close();
                    }
                    // retry
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create rabbitmq channel for producer {} fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }

        this.name = name;
        this.exchangeName = config.getExchangeName();
        this.routingKey = config.getRoutingKey();
        this.channel = channel;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RabbitMQ producer {} build success, exchange: {}, routing key: {}",
                    name, exchangeName, routingKey);
        }
    }

    @Override
    public void sendMessage(Message message) {
        if (executor() == null) {
            throw new IllegalStateException("Producer " + name + " has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message);
        } else {
            executor().submit(() -> sendMessageInner(message));
        }
    }

    private void sendMessageInner(Message message) {
        byte[] msg = message.encode();
        long id = channel.getNextPublishSeqNo();
        resendCache.put(id, message);
        try {
            messageRetryer.call(() -> {
                // IOException will be handled in RetryException catch block
                channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, msg);
                return null;
            });
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Producer {} send message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, message, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
    }

    @Override
    public void sendMessage(Message message, long delay, TimeUnit unit) {
        if (executor() == null) {
            throw new IllegalStateException("Producer " + name + " has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message, delay, unit);
        } else {
            executor().submit(() -> sendMessageInner(message, delay, unit));
        }
    }

    private void sendMessageInner(Message message, long delay, TimeUnit unit) {
        long delayMills = TimeUnit.MILLISECONDS.convert(delay, unit);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(2)
                .expiration(String.valueOf(delayMills))
                .build();
        byte[] msg = message.encode();

        long id = channel.getNextPublishSeqNo();
        resendCache.put(id, message, delayMills);
        try {
            messageRetryer.call(() -> {
                // IOException will be handled in RetryException catch block
                channel.basicPublish(exchangeName, routingKey, properties, msg);
                return null;
            });
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Producer {} send message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, message, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
    }

    @Override
    public void startResendTask() {
        if (executor() == null) {
            throw new IllegalStateException("Producer " + name + " has not initialized success, executor is null");
        }
        executor().scheduleAtFixedRate(this::resendNackMessages, WAIT_ACK_MILLIS, WAIT_ACK_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        try {
            this.channel.close();
        } catch (IOException | TimeoutException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Close channel {} fail", channel);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close producer {}", name);
        }
    }

    private void resendNackMessages() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, ResendMessage>> itr = resendCache.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Long, ResendMessage> entry = itr.next();
            ResendMessage message = entry.getValue();

            // valid, resend
            if (message.sendTime + WAIT_ACK_MILLIS > now) {
                if (message.delay == 0) {
                    executor().submit(() -> {
                        sendMessage(message.content);
                        resendCache.delete(entry.getKey());
                    });
                } else {
                    final long newDelay = Math.max(0, message.sendTime + message.delay - now);
                    executor().submit(() -> {
                        sendMessage(message.content, newDelay, TimeUnit.MILLISECONDS);
                        resendCache.delete(entry.getKey());
                    });
                }
            } else if (message.sendTime + INVALID_MILLIS > now) {
                // invalid, remove
                itr.remove();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Message {} send fail, remove it from resend cache, invalid millis: {}",
                            message.content, INVALID_MILLIS);
                }
            }
        }
    }

    private static class ResendCache {
        /**
         * Use sorted map to store unconfirmed messages.
         * Whatever single or multiple ack, skip list can
         * guarantee good performance.
         * TODO: implement a normal skip list for single thread env.
         */
        private final SortedMap<Long, ResendMessage> unconfirmedMap = new ConcurrentSkipListMap<>();

        void put(Long uniqueId, Message message) {
            unconfirmedMap.put(uniqueId, ResendMessage.create(message));
        }

        void put(Long uniqueId, Message message, long delay) {
            unconfirmedMap.put(uniqueId, ResendMessage.create(message, delay));
        }

        void delete(long uniqueId) {
            delete(uniqueId, false);
        }

        void delete(long uniqueId, boolean multiple) {
            if (!multiple) {
                unconfirmedMap.remove(uniqueId);
            } else {
                unconfirmedMap.headMap(uniqueId + 1).clear();
            }
        }

        Set<Map.Entry<Long, ResendMessage>> entrySet() {
            return unconfirmedMap.entrySet();
        }
    }

    private static class ResendMessage {
        private final Message content;
        private final long delay;
        private final long sendTime;

        ResendMessage(final Message content, final long delay) {
            this.content = content;
            this.delay = delay;
            this.sendTime = System.currentTimeMillis();
        }

        static ResendMessage create(final Message content) {
            return new ResendMessage(content, 0);
        }

        static ResendMessage create(final Message content, final long delay) {
            return new ResendMessage(content, delay);
        }
    }
}
