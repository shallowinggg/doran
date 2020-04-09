package com.shallowinggg.doran.common;

import com.shallowinggg.doran.common.util.Assert;

import java.util.Objects;

/**
 * Special config for RabbitMQ.
 *
 * @author shallowinggg
 */
public class RabbitMQConfig extends MQConfig {
    // producer

    /**
     * The name of rabbitmq exchange, used for rabbitmq producer.
     */
    private String exchangeName;

    /**
     * Routing key used for rabbitmq producer.
     */
    private String routingKey;

    // consumer

    /**
     * The name of rabbitmq queue, used for rabbitmq consumer.
     */
    private String queueName;

    // add this feature
    // private String messageProperties;

    public RabbitMQConfig() {
        super(MQType.RabbitMQ);
    }

    @Override
    public boolean equalsIgnoreThreadNum(MQConfig other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        RabbitMQConfig that = (RabbitMQConfig) other;
        if (getThreadNum() != that.getThreadNum()) {
            return false;
        }

        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getUri(), that.getUri()) &&
                exchangeName.equals(that.exchangeName) &&
                queueName.equals(that.queueName) &&
                routingKey.equals(that.routingKey);
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        Assert.notNull(exchangeName);
        this.exchangeName = exchangeName;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        Assert.notNull(queueName);
        this.queueName = queueName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        Assert.notNull(routingKey);
        this.routingKey = routingKey;
    }

    @Override
    public String toString() {
        return "RabbitMQConfig{" +
                "exchangeName='" + exchangeName + '\'' +
                ", queueName='" + queueName + '\'' +
                ", routingKey='" + routingKey + '\'' +
                "} " + super.toString();
    }
}
