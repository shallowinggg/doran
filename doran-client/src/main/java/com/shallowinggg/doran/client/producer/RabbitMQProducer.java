package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.common.MQConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class RabbitMQProducer extends AbstractBuiltInProducer {
    private final Channel channel;
    private String exchangeName;
    private String key;

    public RabbitMQProducer(final MQConfig config) {
        Connection connection = ConnectionFactoryCache.getInstance().getRabbitMQConnection(config);
        try {
            Channel channel = connection.createChannel();

            this.channel = channel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String sendMessage(Message message) {
        byte[] msg = message.encode();
        try {
            channel.basicPublish(exchangeName, key, MessageProperties.PERSISTENT_TEXT_PLAIN, msg);
        } catch (IOException e) {
            //TODO: retry
        }
        return null;
    }

    @Override
    public String sendMessage(Message message, int delay, TimeUnit unit) {
        return null;
    }
}
