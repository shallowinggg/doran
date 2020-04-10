package com.shallowinggg.doran.server.web.service;

import com.shallowinggg.doran.common.EmptyMQConfig;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.server.web.dao.ActiveConfigDao;
import com.shallowinggg.doran.server.web.dao.MQConfigDao;
import com.shallowinggg.doran.server.web.entity.ActiveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author shallowinggg
 */
@Service
public class MQConfigServiceImpl implements MQConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQConfigServiceImpl.class);

    private static final Supplier<EmptyMQConfig> EMPTY_MQ_CONFIG = EmptyMQConfig::new;

    private final Map<String, MQConfig> mqConfigCache;
    /**
     * This executor is provided to check if there has updated MQ config but not
     * updated in this manager. If has, it will do compensation.
     */
    private final ScheduledExecutorService compensateExecutor;

    private final MQConfigDao mqConfigDao;
    private final ActiveConfigDao activeConfigDao;

    @Autowired
    public MQConfigServiceImpl(MQConfigDao mqConfigDao,
                               ActiveConfigDao activeConfigDao) {
        this.mqConfigDao = mqConfigDao;
        this.activeConfigDao = activeConfigDao;
        this.mqConfigCache = new ConcurrentHashMap<>(32);
        this.compensateExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryImpl("mqConfigCompensateThread_"));
    }

    @Override
    public boolean insertMQConfig(MQConfig config) {
        boolean result = mqConfigDao.insertMQConfig(config);
        if (result) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Add new config {} success", config);
            }
        }
        return result;
    }

    @Override
    public void deleteMQConfig(String configName, MQType mqType) {

    }

    @Override
    public void updateMQConfig(MQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        final String configName = config.getName();
        mqConfigDao.updateMQConfig(config);
        MQConfig oldConfig = mqConfigCache.remove(configName);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Update config {} success, old config: {}, new config: {}",
                    configName, oldConfig, config);
        }
        compensateExecutor.schedule(() -> mqConfigCache.remove(configName), 3, TimeUnit.SECONDS);
    }

    @Override
    public MQConfig selectMQConfig(String configName) {
        Assert.hasText(configName, "'configName' must has text");

        if (mqConfigCache.containsKey(configName)) {
            return mqConfigCache.get(configName);
        }

        ActiveConfig activeConfig = activeConfigDao.selectByName(configName);
        MQConfig config = null;
        if (activeConfig != null) {
            MQType type = activeConfig.getType();
            config = mqConfigDao.selectMQConfig(configName, type);
        }
        if (config == null) {
            config = EMPTY_MQ_CONFIG.get();
        }

        mqConfigCache.putIfAbsent(configName, config);
        return config;
    }

    @Override
    public void activateMQConfig(String configName, MQType mqType) {
        Assert.hasText(configName, "'configName' must has text");
        Assert.notNull(mqType, "'mqType' must not be null");

        MQConfig config = mqConfigDao.selectMQConfig(configName, mqType);
        if(config == null) {
            return;
        }

        ActiveConfig activeConfig = new ActiveConfig(configName, mqType);
        activeConfigDao.updateActiveConfig(activeConfig);
        mqConfigCache.put(configName, config);
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("Active config {}, mq: {}", configName, mqType);
        }
    }

}
