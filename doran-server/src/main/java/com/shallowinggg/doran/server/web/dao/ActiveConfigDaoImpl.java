package com.shallowinggg.doran.server.web.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shallowinggg.doran.server.web.entity.ActiveConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;

/**
 * Redis implementation for interface {@link ActiveConfigDao}.
 * <p>
 * Use a hash key {@link #KEY_NAME} to store all active mq config.
 * Its field is {@link com.shallowinggg.doran.common.MQType},
 * value is {@link ActiveConfig}'s json representation.
 *
 * @author shallowinggg
 */
@Repository
public class ActiveConfigDaoImpl implements ActiveConfigDao {
    private static final String KEY_NAME = "active-configs";
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveConfigDaoImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final BoundHashOperations<String, String, String> hashOps;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public ActiveConfigDaoImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.boundHashOps(KEY_NAME);
    }

    @Override
    @Nullable
    public ActiveConfig selectByName(String name) {
        String json = hashOps.get(name);
        if (json != null) {
            try {
                return MAPPER.readValue(json, ActiveConfig.class);
            } catch (IOException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Deserialize active config {} fail, json: {}",
                            name, json, e);
                }
            }
        }
        return null;
    }

    @Override
    public boolean insertActiveConfig(ActiveConfig config) throws JsonSerializeException {
        String name = config.getName();
        String json;
        try {
            json = MAPPER.writeValueAsString(config);
            Boolean result = hashOps.putIfAbsent(name, json);
            return result != null && result;
        } catch (JsonProcessingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Serialize mq config {} fail", config, e);
            }
            throw new JsonSerializeException(config, e);
        }
    }

    @Override
    public void deleteActiveConfig(String name) {
        hashOps.delete(name);
    }

    @Override
    public void updateActiveConfig(ActiveConfig config) throws JsonSerializeException {
        String name = config.getName();
        String json;
        try {
            json = MAPPER.writeValueAsString(config);
            hashOps.put(name, json);
        } catch (JsonProcessingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Serialize mq config {} fail", config, e);
            }
            throw new JsonSerializeException(config, e);
        }
    }
}
