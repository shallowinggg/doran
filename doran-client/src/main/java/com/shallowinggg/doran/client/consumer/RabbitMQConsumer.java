package com.shallowinggg.doran.client.consumer;

import com.rabbitmq.client.*;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.client.RetryCountExhaustedException;
import com.shallowinggg.doran.client.producer.ConnectionFactoryCache;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import com.shallowinggg.doran.common.util.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class RabbitMQConsumer extends AbstractBuiltInConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private Channel channel;
    private DefaultConsumer consumer;
    private final String queueName;
    private final Retryer<Message> messageRetryer = RetryerBuilder.<Message>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();

    public RabbitMQConsumer(final RabbitMQConfig config, Set<MessageListener> listeners) {
        super(listeners);
        Assert.notNull(config, "'config' must not be null");
        this.queueName = config.getQueueName();

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
                    innerChannel.basicQos(64);
                    if (CollectionUtils.isNotEmpty(getMessageListeners())) {
                        this.consumer = new DefaultConsumer(innerChannel) {
                            @Override
                            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                                boolean ack = false;
                                final Message message = Message.decode(body);
                                long id = envelope.getDeliveryTag();
                                for (MessageListener listener : getMessageListeners()) {
                                    if (listener.onMessage(message)) {
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("Message {} consume success, listener: {}", id, listener);
                                        }
                                        ack = true;
                                    }
                                }
                                if (ack) {
                                    getChannel().basicAck(id, false);
                                } else {
                                    if (LOGGER.isErrorEnabled()) {
                                        LOGGER.error("Message {} consume fail, cause: no MessageListener can handle it", id);
                                    }
                                    getChannel().basicNack(id, false, false);
                                }
                            }
                        };
                        innerChannel.basicConsume(queueName, false, consumer);
                    } else {
                        this.consumer = null;
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

        this.channel = channel;
    }

    @Override
    public Message receive() {
        if (this.consumer != null) {
            throw new IllegalStateException("Rabbitmq consumer consume async");
        }

        try {
            return messageRetryer.call(() -> {
                try {
                    GetResponse response = channel.basicGet(queueName, true);
                    if (response != null) {
                        return Message.decode(response.getBody());
                    }
                    return null;
                } catch (IOException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Receive message fail, retry", e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
            return null;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Receive message fail, retry count {} has exhausted",
                        attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("RabbitMQ don't support this operations");
    }
}
