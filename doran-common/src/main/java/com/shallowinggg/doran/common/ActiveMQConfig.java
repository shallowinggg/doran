package com.shallowinggg.doran.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

/**
 * Special config for ActiveMQ.
 *
 * @author shallowinggg
 */
public class ActiveMQConfig extends MQConfig {

    private final String destinationName;

    private final String destinationType;

    @Nullable
    private final String selector;

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                          long timestamp, String json) throws JsonProcessingException {
        super(name, type, uri, username, password, threadNum, timestamp);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode extFields = mapper.readTree(json);
        String destinationName = extFields.get("destinationName").asText();
        String destinationType = extFields.get("destinationType").asText();
        String selector = extFields.get("selector").asText();
        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.selector = selector;
    }

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, long timestamp,
                          String destinationName, String destinationType, @Nullable String selector) {
        super(name, type, uri, username, password, timestamp);

        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.selector = selector;
    }

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                             long timestamp, String destinationName, String destinationType, @Nullable String selector) {
        super(name, type, uri, username, password, threadNum, timestamp);

        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.selector = selector;
    }

    @Override
    public String extFieldsToJson() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("destinationName", getDestinationName());
        node.put("destinationType", getDestinationType());
        node.put("selector", getSelector());
        return node.toString();
    }


    public String getDestinationName() {
        return destinationName;
    }

    public String getDestinationType() {
        return destinationType;
    }

    @Nullable
    public String getSelector() {
        return selector;
    }
}
