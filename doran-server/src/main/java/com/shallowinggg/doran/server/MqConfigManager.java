package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shallowinggg
 */
public class MqConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqConfigManager.class);
    private final ServerController controller;
    private final Map<String, MqConfig> mqConfigMap;

    public MqConfigManager(final ServerController controller) {
        this.controller = controller;
        this.mqConfigMap = new ConcurrentHashMap<>(32);
    }

    public void addMqConfig(MqConfig config) {
        final String configName = config.getName();
        if (mqConfigMap.containsKey(configName)) {
            mqConfigMap.put(configName, config);
            LOGGER.info("Add MQ Config {} success", config);
        } else {
            LOGGER.warn("MQ Config {} has added before", configName);
        }
    }

    public boolean updateMqConfig(MqConfig newConfig) {
        final String configName = newConfig.getName();
        MqConfig oldConfig = mqConfigMap.replace(configName, newConfig);
        if (oldConfig != null) {
            LOGGER.info("Update MQ Config {} success, old config: {}, new config: {}",
                    configName, oldConfig, newConfig);
            return true;
        } else {
            LOGGER.warn("Update MQ Config {} fail, it is not exist", configName);
            return false;
        }
    }

    public MqConfig getConfig(String configName) {
        Assert.hasText(configName);
        return mqConfigMap.get(configName);
    }

    public boolean containsConfig(String configName) {
        Assert.hasText(configName);
        return mqConfigMap.containsKey(configName);
    }
}
