package com.shallowinggg.doran.server.transport;

import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.MQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author shallowinggg
 */
public class MQConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQConfigManager.class);
    private final Map<String, MQConfig> mqConfigMap;

    /**
     * This executor is provided to check if there has updated MQ config but not
     * updated in this manager. If has, it will do compensation.
     */
    private final ScheduledExecutorService compensateExecutor;

    public MQConfigManager() {
        this.mqConfigMap = new ConcurrentHashMap<>(32);
        this.compensateExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryImpl("mqConfigCompensateThread_"));
    }

    public void addMqConfig(MQConfig config) {
        final String configName = config.getName();
        if (mqConfigMap.containsKey(configName)) {
            mqConfigMap.put(configName, config);
            LOGGER.info("Add MQ Config {} success", config);
        } else {
            LOGGER.warn("MQ Config {} has added before", configName);
        }
    }

    public boolean updateMqConfig(MQConfig newConfig) {
        final String configName = newConfig.getName();
        MQConfig oldConfig = mqConfigMap.replace(configName, newConfig);
        if (oldConfig != null) {
            LOGGER.info("Update MQ Config {} success, old config: {}, new config: {}",
                    configName, oldConfig, newConfig);
            return true;
        } else {
            LOGGER.warn("Update MQ Config {} fail, it is not exist", configName);
            return false;
        }
    }

    public MQConfig getConfig(String configName) {
        Assert.hasText(configName);
        return mqConfigMap.get(configName);
    }

    public boolean containsConfig(String configName) {
        Assert.hasText(configName);
        return mqConfigMap.containsKey(configName);
    }
}
