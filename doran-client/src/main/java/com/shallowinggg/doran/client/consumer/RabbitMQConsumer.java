package com.shallowinggg.doran.client.consumer;

import com.rabbitmq.client.*;
import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.RetryCountExhaustedException;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import com.shallowinggg.doran.common.util.retry.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class RabbitMQConsumer extends AbstractBuiltInConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private final String name;
    private final Channel channel;
    private DefaultConsumer consumer;
    private final String queueName;
    private final Retryer<Message> messageRetryer = RetryerBuilder.<Message>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(2))
            .build();

    public RabbitMQConsumer(String name, RabbitMQConfig config,
                            @Nullable ThreadPoolExecutor executor,
                            @Nullable List<MessageListener> listeners) {
        super(executor, listeners);
        Assert.hasText(name, "'name' must has text");
        Assert.notNull(config, "'config' must not be null");
        this.queueName = config.getQueueName();
        this.name = name;

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
                    // TODO: configurable
                    innerChannel.basicQos(64);
                    if (CollectionUtils.isNotEmpty(getMessageListeners())) {
                        this.consumer = new DefaultConsumer(innerChannel) {
                            @Override
                            public void handleDelivery(String consumerTag, Envelope envelope,
                                                       AMQP.BasicProperties properties, byte[] body) {
                                executor().execute(() -> {
                                    boolean ack = false;
                                    final Message message = Message.decode(body);
                                    long id = envelope.getDeliveryTag();
                                    for (MessageListener listener : getMessageListeners()) {
                                        if (listener.accept(message)) {
                                            listener.onMessage(message);
                                            ack = true;
                                        }
                                    }

                                    try {
                                        if (ack) {
                                            if (LOGGER.isDebugEnabled()) {
                                                LOGGER.debug("RabbitMQ consumer {} consume message {} success",
                                                        name, message);
                                            }
                                            getChannel().basicAck(id, false);
                                        } else {
                                            if (LOGGER.isErrorEnabled()) {
                                                LOGGER.error("RabbitMQ consumer {} consume message {} fail, delivery tag: {}",
                                                        name, message, id);
                                            }
                                            getChannel().basicNack(id, false, false);
                                        }
                                    } catch (IOException e) {
                                        if (LOGGER.isErrorEnabled()) {
                                            LOGGER.error("Send response to broker fail, consumer: {} delivery tag: {}, ack: {}",
                                                    name, id, ack);
                                        }
                                    }
                                });
                            }
                        };
                        innerChannel.basicConsume(queueName, false, consumer);
                    } else {
                        this.consumer = null;
                    }
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
                LOGGER.error("Create rabbitmq channel for consumer {} fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }

        this.channel = channel;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RabbitMQ consumer {} build success, queue: {}", name, queueName);
        }
    }

    @Override
    public Message receive() {
        if (this.consumer != null) {
            throw new IllegalStateException("RabbitMQ consumer " + name + "is configured as async mode");
        }

        try {
            return messageRetryer.call(() -> {
                GetResponse response = channel.basicGet(queueName, true);
                if (response != null) {
                    return Message.decode(response.getBody());
                }
                return null;
            });
        } catch (ExecutionException e) {
            // won't goto this branch
            assert false;
            return null;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("RabbitMQ consumer {} receive message fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("RabbitMQ don't support this operation");
    }
}
