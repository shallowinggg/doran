package com.shallowinggg.doran.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shallowinggg.doran.common.*;

/**
 * @author shallowinggg
 */
public abstract class PojoHeaderConverter {

    public static <T extends MQConfig> void mqConfig2ResponseHeader(T config,
                                                                    RequestMQConfigResponseHeader header) {
        Assert.notNull(config, "'config' must not be null");
        Assert.notNull(header, "'header' must not be null");
        header.setName(config.getName());
        header.setType(config.getType().name());
        header.setUri(config.getUri());
        header.setUsername(config.getUsername());
        header.setPassword(config.getPassword());
        header.setThreadNum(config.getThreadNum());
        header.setTimestamp(config.getTimestamp());
        header.setExtFieldsJson(config.extFieldsToJson());
    }

    public static MQConfig responseHeader2MQConfig(RequestMQConfigResponseHeader header)
            throws JsonProcessingException {
        Assert.notNull(header, "'header' must not be null");
        MQType type = MQType.parse(header.getType());
        switch (type) {
            case RabbitMQ:
                return new RabbitMQConfig(header.getName(), type, header.getUri(), header.getUsername(),
                        header.getPassword(), header.getThreadNum(), header.getTimestamp(), header.getExtFieldsJson());
            case ActiveMQ:
                return new ActiveMQConfig(header.getName(), type, header.getUri(), header.getUsername(),
                        header.getPassword(), header.getThreadNum(), header.getTimestamp(), header.getExtFieldsJson());
            default:
                return new EmptyMQConfig();
        }
    }
}
