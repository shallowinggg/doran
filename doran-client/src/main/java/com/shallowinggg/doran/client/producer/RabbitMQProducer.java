package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.*;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.client.RetryCountExhaustedException;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.util.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private final Retryer<Channel> retryer = RetryerBuilder.<Channel>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .withWaitStrategy(WaitStrategies.fibonacciWait())
            .build();

    public RabbitMQProducer(final RabbitMQConfig config) {
        Connection connection = ConnectionFactoryCache.getInstance().getRabbitMQConnection(config);
        final String name = config.getName();
        Channel channel = null;
        try {
            channel = retryer.call(() -> {
                Channel innerChannel = null;
                try {
                    innerChannel = connection.createChannel();
                    innerChannel.confirmSelect();
                    innerChannel.addConfirmListener(new ConfirmListener() {
                        @Override
                        public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                            // 维护一个优先级队列
                        }

                        @Override
                        public void handleNack(long deliveryTag, boolean multiple) throws IOException {

                        }
                    });
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Create rabbitmq channel for config {} success", name);
                    }
                    return innerChannel;
                } catch (IOException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Create rabbitmq channel for config {} fail, retry it", name, e);
                    }
                    if(innerChannel != null) {
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
                LOGGER.error("Create rabbitmq channel for config {} fail, cause: retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }

        this.exchangeName = config.getExchangeName();
        this.routingKey = config.getRoutingKey();
        this.channel = channel;
    }

    @Override
    public void sendMessage(Message message) {
        byte[] msg = message.encode();
        try {
            channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, msg);
        } catch (IOException e) {
            //TODO: retry
        }
    }

    @Override
    public void sendMessage(Message message, int delay, TimeUnit unit) {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(2)
                .expiration(String.valueOf(TimeUnit.MILLISECONDS.convert(delay, unit)))
                .build();
        byte[] msg = message.encode();
        try {
            channel.basicPublish(exchangeName, routingKey, properties, msg);
        } catch (IOException e) {
            //TODO: retry
        }
    }

    @Override
    public void close() {
        try {
            this.channel.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
