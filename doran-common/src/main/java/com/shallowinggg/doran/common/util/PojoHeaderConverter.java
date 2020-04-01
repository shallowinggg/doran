package com.shallowinggg.doran.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shallowinggg.doran.common.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author shallowinggg
 */
public final class PojoHeaderConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PojoHeaderConverter() {
    }

    public static <T extends MQConfig> void mqConfig2ResponseHeader(@NotNull T config,
                                                                    @NotNull RequestMQConfigResponseHeader header) {
        Assert.notNull(config, "'config' must not be null");
        Assert.notNull(header, "'header' must not be null");
        try {
            String json = MAPPER.writeValueAsString(config);
            header.setType(config.getType().name());
            header.setConfig(json);
        } catch (JsonProcessingException e) {
            // won't happen
        }
    }

    public static MQConfig responseHeader2MQConfig(RequestMQConfigResponseHeader header)
            throws JsonProcessingException {
        Assert.notNull(header, "'header' must not be null");
        MQType type = MQType.parse(header.getType());
        switch (type) {
            case RabbitMQ:
                return MAPPER.readValue(header.getConfig(), RabbitMQConfig.class);
            case ActiveMQ:
                return MAPPER.readValue(header.getConfig(), ActiveMQConfig.class);
            default:
                return new EmptyMQConfig();
        }
    }
}
