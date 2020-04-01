package com.shallowinggg.doran.server.web.dao;

import com.shallowinggg.doran.common.MQConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author shallowinggg
 */
@Repository
public class MQConfigDaoImpl implements MQConfigDao {
    @Autowired
    private RedisTemplate<String, String> configRedisTemplate;


    private HashOperations<String, String, String> hashOperations;

    @Override
    public int addMQConfig(MQConfig config) {
        
        return 0;
    }

    @Override
    public int deleteMQConfig(String mqName, String mqType) {
        return 0;
    }

    @Override
    public int updateMQConfig(MQConfig config) {
        return 0;
    }

    @Override
    public MQConfig selectMQConfig(String mqName, String mqType) {
        return null;
    }
}
