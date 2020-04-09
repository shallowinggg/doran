package com.shallowinggg.doran.server.web.service;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.server.web.dao.ActiveConfigDao;
import com.shallowinggg.doran.server.web.dao.JsonSerializeException;
import com.shallowinggg.doran.server.web.dao.MQConfigDao;
import org.jetbrains.annotations.Nullable;
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
    public boolean insertMQConfig(MQConfig config) {
        return false;
    }

    @Override
    public void deleteMQConfig(String mqName, MQType mqType) {

    }

    @Override
    public void updateMQConfig(MQConfig config) throws JsonSerializeException {

    }

    @Nullable
    @Override
    public MQConfig selectMQConfig(String mqName, MQType mqType) {
        return null;
    }

    @Override
    public void activateMQConfig() {

    }


}
