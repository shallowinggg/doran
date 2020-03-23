package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.common.RabbitMQConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author shallowinggg
 */
public class RabbitMQProducer extends AbstractBuiltInProducer {
    private final Channel channel;
    private final String exchangeName;
    private final String routingKey;

    public RabbitMQProducer(final RabbitMQConfig config) {
        Connection connection = ConnectionFactoryCache.getInstance().getRabbitMQConnection(config);
        try {
            Channel channel = connection.createChannel();
            channel.confirmSelect();
            this.exchangeName = config.getExchangeName();
            this.routingKey = config.getRoutingKey();
            this.channel = channel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sendMessage(Message message) {
        byte[] msg = message.encode();
        try {
            channel.getNextPublishSeqNo();
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
