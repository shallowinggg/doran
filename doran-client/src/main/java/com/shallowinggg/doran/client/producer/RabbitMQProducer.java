package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.common.Domain;
import com.shallowinggg.doran.common.MqConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class RabbitMQProducer extends AbstractBuiltInProducer {
    private final Channel channel;
    private final String exchangeName;
    private final String key;

    public RabbitMQProducer(final MqConfig config) {
        Connection connection = ConnectionFactoryCache.getInstance().getRabbitMQConnection(config);
        try {
            Channel channel = connection.createChannel();
            String exchangeName = null;
            String key = null;
            if(Domain.PTP == config.getDomain()) {
                exchangeName = config.getDomainName();
                String queueName = exchangeName + "_queue";
                key = exchangeName + "_key";
                channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, true, false, null);
                channel.queueDeclare(queueName, true, false, false, null);
                channel.queueBind(queueName, exchangeName, key);
            } else {
                //TODO: broadcast
            }

            this.exchangeName = exchangeName;
            this.key = key;
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
