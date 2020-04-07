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
                                // TODO: use executor() to handle it when thread unsafe skip list can use
                                resendCache.delete(deliveryTag);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("{} send message {} success", name, deliveryTag);
                                }
                            } else {
                                resendCache.delete(deliveryTag, true);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("{} send message {} and earlier success", name, deliveryTag);
                                }
                            }
                        }

                        @Override
                        public void handleNack(long deliveryTag, boolean multiple) {
                            if (!multiple) {
                                if (LOGGER.isWarnEnabled()) {
                                    LOGGER.warn("{} send message {} fail, server nack, wait for resend", name, deliveryTag);
                                }
                            } else {
                                if (LOGGER.isWarnEnabled()) {
                                    LOGGER.warn("{} send message {} and earlier, server nack, wait for resend", name, deliveryTag);
                                }
                            }
                        }
                    });
                    innerChannel.addReturnListener(r -> {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("{} send message {} fail, message can't be routed, exchange: {}, routingKey: {}",
                                    name, r.getBody(), r.getExchange(), r.getRoutingKey());
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
            throw new IllegalStateException("Producer '" + name + "' has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message);
        } else {
            executor().submit(() -> sendMessageInner(message));
        }
    }

    private void sendMessageInner(Message message) {
        byte[] content = message.encode();
        doSendMessage(message, content, 0);
    }

    @Override
    public void sendMessage(Message message, long delay, TimeUnit unit) {
        if (executor() == null) {
            throw new IllegalStateException("Producer '" + name + "' has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message, delay, unit);
        } else {
            executor().submit(() -> sendMessageInner(message, delay, unit));
        }
    }

    private void sendMessageInner(Message message, long delay, TimeUnit unit) {
        long delayMillis = TimeUnit.MILLISECONDS.convert(delay, unit);
        byte[] content = message.encode();
        doSendMessage(message, content, delayMillis);
    }

    private void doSendMessage(Message msg, byte[] content, long delay) {
        AMQP.BasicProperties properties;
        if (delay == 0) {
            properties = MessageProperties.PERSISTENT_TEXT_PLAIN;
        } else {
            properties = new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2)
                    .expiration(String.valueOf(delay))
                    .build();
        }

        long id = channel.getNextPublishSeqNo();
        resendCache.put(id, msg, content, delay);
        try {
            messageRetryer.call(() -> {
                // IOException will be handled in RetryException catch block
                channel.basicPublish(exchangeName, routingKey, properties, content);
                return null;
            });
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("'{}' send message {}, content: {}", name, id, msg);
            }
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("'{}' send message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, msg, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
    }

    @Override
    public void startResendTask() {
        if (executor() == null) {
            throw new IllegalStateException("Producer '" + name + "' has not initialized success, executor is null");
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
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close producer '{}' success", name);
        }
    }

    private void resendNackMessages() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, ResendMessage>> itr = resendCache.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Long, ResendMessage> entry = itr.next();
            ResendMessage message = entry.getValue();

            final long id = entry.getKey();
            final long sendTime = message.sendTime;
            final long delay = message.delay;
            final Message msg = message.origin;
            final byte[] content = message.content;

            // valid, resend
            if (sendTime + WAIT_ACK_MILLIS > now) {
                if (delay == 0) {
                    executor().submit(() -> {
                        // check again
                        if (resendCache.needResend(id)) {
                            doResendMessage(msg, content, delay, sendTime);
                            resendCache.delete(id);
                        }
                    });
                } else {
                    final long newDelay = Math.max(0, sendTime + delay - now);
                    executor().submit(() -> {
                        // check again
                        if (resendCache.needResend(id)) {
                            doResendMessage(msg, content, newDelay, sendTime);
                            resendCache.delete(id);
                        }
                    });
                }
            } else if (sendTime + INVALID_MILLIS > now) {
                // invalid, remove
                itr.remove();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Message {} send fail, remove it from resend cache, invalid millis: {}",
                            msg, INVALID_MILLIS);
                }
            }
        }
    }

    private void doResendMessage(Message msg, byte[] content, long delay, long sendTime) {
        AMQP.BasicProperties properties;
        if (delay == 0) {
            properties = MessageProperties.PERSISTENT_TEXT_PLAIN;
        } else {
            properties = new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2)
                    .expiration(String.valueOf(delay))
                    .build();
        }

        long id = channel.getNextPublishSeqNo();
        resendCache.put(id, msg, content, delay, sendTime);
        try {
            messageRetryer.call(() -> {
                // IOException will be handled in RetryException catch block
                channel.basicPublish(exchangeName, routingKey, properties, content);
                return null;
            });
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("'{}' send message {}, content: {}", name, id, msg);
            }
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("'{}' resend message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, msg, attempt.getAttemptNumber(), attempt.getExceptionCause());
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

        /**
         * Resend task won't be executed immediately, during
         * this time message may be ack, so it should be
         * checked again.
         *
         * @param uniqueId id for last send
         * @return {@code true} if message has been ack
         */
        boolean needResend(Long uniqueId) {
            return unconfirmedMap.containsKey(uniqueId);
        }

        void put(Long uniqueId, Message message, byte[] content, long delay) {
            unconfirmedMap.put(uniqueId, ResendMessage.create(message, content, delay));
        }

        void put(Long uniqueId, Message message, byte[] content, long delay, long sendTime) {
            unconfirmedMap.put(uniqueId, ResendMessage.create(message, content, delay, sendTime));
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

    /**
     * Resend structure
     */
    private static class ResendMessage {
        /**
         * Message that need to resend
         */
        private final Message origin;

        /**
         * Maybe increase memory cost, but it can decrease
         * a lot {@link Message#encode()} operations.
         */
        private final byte[] content;

        /**
         * Delay millis for message, if it not,
         * this value will be 0.
         */
        private final long delay;

        /**
         * Send time at the first time
         */
        private final long sendTime;

        /**
         * Build resend structure for message that send fail at the first time.
         *
         * @param origin  concrete message
         * @param content byte representation for message content
         * @param delay   delay millis for message, if it isn't a delay message, this should be 0
         */
        ResendMessage(final Message origin, final byte[] content, final long delay) {
            this.origin = origin;
            this.content = content;
            this.delay = delay;
            this.sendTime = System.currentTimeMillis();
        }

        /**
         * For resend message what send fail more than once,
         * send time should be its original send time,
         * in case it resend forever.
         *
         * @param origin   concrete message
         * @param content  byte representation for message content
         * @param delay    delay millis for message, if it isn't a delay message, this should be 0
         * @param sendTime first send time
         */
        ResendMessage(final Message origin, final byte[] content, final long delay, final long sendTime) {
            this.origin = origin;
            this.content = content;
            this.delay = delay;
            this.sendTime = sendTime;
        }

        static ResendMessage create(final Message origin, final byte[] content, final long delay) {
            return new ResendMessage(origin, content, delay);
        }

        static ResendMessage create(final Message origin, final byte[] content, final long delay,
                                    final long sendTime) {
            return new ResendMessage(origin, content, delay, sendTime);
        }
    }
}
