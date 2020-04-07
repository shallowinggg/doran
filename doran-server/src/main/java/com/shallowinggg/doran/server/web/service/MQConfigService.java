package com.shallowinggg.doran.server.web.service;

import com.shallowinggg.doran.common.MQConfig;

/**
 * @author shallowinggg
 */
public interface MQConfigService {

    void insertMQConfig(MQConfig config);

    void activateMQConfig();


}
