package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.*;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.RetryCountExhaustedException;
import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author shallowinggg
 */
public class RabbitMQProducer extends AbstractBuiltInProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQProducer.class);
    private final Channel channel;
    private final String exchangeName;
    private final String routingKey;
    private final ResendCache resendCache = new ResendCache();
    private final Retryer<Void> messageRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();

    public RabbitMQProducer(final RabbitMQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        Connection connection = ConnectionFactoryCache.getInstance().getRabbitMQConnection(config);
        final String name = config.getName();
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
                                    LOGGER.warn("Message {}  and earlier are nack, wait for resend", deliveryTag);
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
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Create rabbitmq channel for config {} success", name);
                    }
                    return innerChannel;
                } catch (IOException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Create rabbitmq channel for config {} fail, retry", name, e);
                    }
                    if (innerChannel != null) {
                        innerChannel.close();
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create rabbitmq channel for config {} fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }

        this.exchangeName = config.getExchangeName();
        this.routingKey = config.getRoutingKey();
        this.channel = channel;
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("RabbitMQ producer build success, exchange: {}, routing key: {}", exchangeName,
                    routingKey);
        }
    }

    @Override
    public void sendMessage(Message message) {
        if (executor() == null) {
            throw new IllegalStateException("Producer has not initialized success, executor is null");
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
                try {
                    channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, msg);
                    return null;
                } catch (IOException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Send message {} fail, retry", id, e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Send message {} fail, content: {}, retry count {} has exhausted",
                        id, message, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    @Override
    public void sendMessage(Message message, long delay, TimeUnit unit) {
        if (executor() == null) {
            throw new IllegalStateException("Producer has not initialized success, executor is null");
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
                try {
                    channel.basicPublish(exchangeName, routingKey, properties, msg);
                    return null;
                } catch (IOException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Send message {} fail, retry", id, e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Send message {} fail, content: {}, retry count {} has exhausted",
                        id, message, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    @Override
    public void startResendTask() {
        if (executor() == null) {
            throw new IllegalStateException("Producer has not initialized success, executor is null");
        }
        executor().scheduleAtFixedRate(this::resendNackMessages, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        try {
            this.channel.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void resendNackMessages() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, ResendMessage> entry : resendCache.entrySet()) {
            ResendMessage message = entry.getValue();
            if (message.sendTime + WAIT_ACK_MILLS > now) {
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
