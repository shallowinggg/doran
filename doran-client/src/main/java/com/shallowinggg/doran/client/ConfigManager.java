package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.EmptyMQConfig;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager {@link MQConfig}s for one client.
 *
 * @author shallowinggg
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final MQConfig NON_EXIST_CONFIG = new EmptyMQConfig();

    private final ClientController controller;

    /**
     * configName -> MQConfig
     */
    private final Map<String, MQConfig> configMap;




    public ConfigManager(final ClientController controller) {
        this.controller = controller;
        this.configMap = new ConcurrentHashMap<>(16);
    }

    @NotNull
    public MQConfig getConfig(String configName, int timeoutMillis) {
        if (configMap.containsKey(configName)) {
            MQConfig config = configMap.get(configName);
            if (config == NON_EXIST_CONFIG) {
                throw new ConfigNotExistException(configName);
            }
            return config;
        }

        // for hotspot jdk8, this is safe
        // TODO: use more compatibility way
        synchronized (configName.intern()) {
            if (configMap.containsKey(configName)) {
                return configMap.get(configName);
            }

            MQConfig config;
            try {
                config = controller.getClientApiImpl().requestConfig(configName, timeoutMillis);
                configMap.put(configName, config);
            } catch (ConfigNotExistException e) {
                configMap.put(configName, NON_EXIST_CONFIG);
                throw e;
            }
            return config;
        }
    }

    public void registerMqConfigs(List<MQConfig> mqConfigs) {
        for (MQConfig config : mqConfigs) {
            this.configMap.putIfAbsent(config.getName(), config);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Register MQ Config {}", config);
            }
        }
    }


}
