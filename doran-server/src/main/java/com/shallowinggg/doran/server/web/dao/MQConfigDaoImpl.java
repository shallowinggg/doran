package com.shallowinggg.doran.server.web.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.common.RabbitMQConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;

/**
 * Redis implementation for interface {@link MQConfigDao}.
 * <p>
 * <pre>
 * Store structure is like this:
 * -------hash------config:${name}-------
 * ----  key: {@link MQType#name()}  ----
 * ----       value: config          ----
 * --------------------------------------
 * </pre>
 * <p>
 * Every config use a hash key like "{@link #KEY_PREFIX} + config name",
 * and same name config can have multi {@link MQType} configuration.
 * So they should be stored in one hash key, use {@link MQType} as field
 * and {@link MQConfig}'s json representation as value.
 *
 * @author shallowinggg
 */
@Repository
public class MQConfigDaoImpl implements MQConfigDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQConfigDaoImpl.class);
    private static final String KEY_PREFIX = "config:";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedisTemplate<String, String> redisTemplate;

    private final HashOperations<String, String, String> hashOps;

    @Autowired
    public MQConfigDaoImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    @Override
    public boolean insertMQConfig(MQConfig config) throws JsonSerializeException {
        String key = KEY_PREFIX + config.getName();
        String type = config.getType().name();
        String json;
        try {
            json = MAPPER.writeValueAsString(config);
            return hashOps.putIfAbsent(key, type, json);
        } catch (JsonProcessingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Serialize mq config {} fail", config, e);
            }
            throw new JsonSerializeException(config, e);
        }
    }

    @Override
    public void deleteMQConfig(String mqName, MQType mqType) {
        String key = KEY_PREFIX + mqName;
        hashOps.delete(key, mqType.name());
    }

    @Override
    public void updateMQConfig(MQConfig config) throws JsonSerializeException {
        String key = KEY_PREFIX + config.getName();
        String type = config.getType().name();
        String json;
        try {
            json = MAPPER.writeValueAsString(config);
            hashOps.put(key, type, json);
        } catch (JsonProcessingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Serialize mq config {} fail", config, e);
            }
            throw new JsonSerializeException(config, e);
        }
    }

    @Override
    @Nullable
    public MQConfig selectMQConfig(String mqName, MQType mqType) {
        String key = KEY_PREFIX + mqName;
        String type = mqType.name();
        String json = hashOps.get(key, type);
        try {
            switch (mqType) {
                case RabbitMQ:
                    return MAPPER.readValue(json, RabbitMQConfig.class);
                case ActiveMQ:
                    return MAPPER.readValue(json, ActiveMQConfig.class);
                default:
                    throw new IllegalArgumentException(type);
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Deserialize mq config {} fail, type: {}, json: {}",
                        mqName, type, json, e);
            }
        }
        return null;
    }
}
