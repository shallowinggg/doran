package com.shallowinggg.doran.server.web.dao;

import com.shallowinggg.doran.common.MQConfig;

/**
 * @author shallowinggg
 */
public interface MQConfigDao {

    int addMQConfig(MQConfig config);

    int deleteMQConfig(String mqName, String mqType);

    int updateMQConfig(MQConfig config);

    MQConfig selectMQConfig(String mqName, String mqType);
}
