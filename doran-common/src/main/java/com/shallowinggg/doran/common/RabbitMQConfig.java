package com.shallowinggg.doran.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Special config for RabbitMQ.
 *
 * @author shallowinggg
 */
public class RabbitMQConfig extends MQConfig {
    /**
     * The name of rabbitmq exchange, used for rabbitmq producer.
     */
    private final String exchangeName;

    /**
     * The name of rabbitmq queue, used for rabbitmq consumer.
     */
    private final String queueName;

    /**
     * Routing key used for rabbitmq producer.
     */
    private final String routingKey;

    // add this feature
    // private String messageProperties;

    public RabbitMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                          long timestamp, String json) throws JsonProcessingException {
        super(name, type, uri, username, password, threadNum, timestamp);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode extFields = mapper.readTree(json);
        String exchangeName = extFields.get("exchangeName").asText();
        String queueName = extFields.get("queueName").asText();
        String routingKey = extFields.get("routingKey").asText();
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.routingKey = routingKey;
    }

    public RabbitMQConfig(String name, MQType type, String uri, String username, String password, long timestamp,
                          String exchangeName, String queueName, String routingKey) {
        super(name, type, uri, username, password, timestamp);
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.routingKey = routingKey;
    }

    public RabbitMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                          long timestamp, String exchangeName, String queueName, String routingKey) {
        super(name, type, uri, username, password, threadNum, timestamp);
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.routingKey = routingKey;
    }

    @Override
    public String extFieldsToJson() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("exchangeName", getExchangeName());
        node.put("queueName", getQueueName());
        node.put("routingKey", getRoutingKey());
        return node.toString();
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
