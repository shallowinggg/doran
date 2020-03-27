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

    private final DestinationType destinationType;

    @Nullable
    private final String clientId;

    @Nullable
    private final String selector;

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                          long timestamp, String json) throws JsonProcessingException {
        super(name, type, uri, username, password, threadNum, timestamp);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode extFields = mapper.readTree(json);
        String destinationName = extFields.get("destinationName").asText();
        DestinationType destinationType = DestinationType.valueOf(extFields.get("destinationType").asText());
        String clientId = extFields.get("clientId").asText();
        String selector = extFields.get("selector").asText();
        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.clientId = clientId;
        this.selector = selector;
    }

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, long timestamp,
                          String destinationName, DestinationType destinationType,
                          @Nullable String clientId, @Nullable String selector) {
        super(name, type, uri, username, password, timestamp);

        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.clientId = clientId;
        this.selector = selector;
    }

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                             long timestamp, String destinationName, DestinationType destinationType,
                          @Nullable String clientId, @Nullable String selector) {
        super(name, type, uri, username, password, threadNum, timestamp);

        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.clientId = clientId;
        this.selector = selector;
    }

    @Override
    public String extFieldsToJson() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("destinationName", getDestinationName());
        node.put("destinationType", getDestinationType().name);
        node.put("clientId", getClientId());
        node.put("selector", getSelector());
        return node.toString();
    }

    public enum DestinationType {
        /**
         * PTP mode
         */
        PTP("PTP"),
        /**
         * Topic mode
         */
        TOPIC("TOPIC");

        private final String name;

        DestinationType(String name) {
            this.name = name;
        }
    }


    public String getDestinationName() {
        return destinationName;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    @Nullable
    public String getSelector() {
        return selector;
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }
}
