package com.shallowinggg.doran.server.web.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shallowinggg.doran.server.web.entity.ActiveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author shallowinggg
 */
@Repository
public class ActiveConfigDaoImpl implements ActiveConfigDao {
    private static final String KEY_NAME = "active-configs";
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveConfigDaoImpl.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ActiveConfig selectByName(String name) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        String json = hashOperations.get(KEY_NAME, name);
        if(json != null) {
            try {
                return MAPPER.readValue(json, ActiveConfig.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean insertActiveConfig(ActiveConfig config) {
        String name = config.getName();
        String json;
        try {
            json = MAPPER.writeValueAsString(config);
            return redisTemplate.opsForHash().putIfAbsent(KEY_NAME, name, json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteActiveConfig(String name) {
        return false;
    }

    @Override
    public boolean updateActiveConfig(ActiveConfig config) {
        return false;
    }
}
