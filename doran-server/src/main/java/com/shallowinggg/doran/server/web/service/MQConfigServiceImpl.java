package com.shallowinggg.doran.server.web.service;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.server.web.dao.ActiveConfigDao;
import com.shallowinggg.doran.server.web.dao.MQConfigDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author shallowinggg
 */
@Service
public class MQConfigServiceImpl implements MQConfigService {
    private final MQConfigDao mqConfigDao;
    private final ActiveConfigDao activeConfigDao;

    @Autowired
    public MQConfigServiceImpl(MQConfigDao mqConfigDao, ActiveConfigDao activeConfigDao) {
        this.mqConfigDao = mqConfigDao;
        this.activeConfigDao = activeConfigDao;
    }

    @Override
    public void insertMQConfig(MQConfig config) {

    }

    @Override
    public void activateMQConfig() {

    }
}
