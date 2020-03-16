package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager {@link MqConfig}s for one client.
 *
 * @author shallowinggg
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final MqConfig NON_EXIST_CONFIG = new MqConfig();

    private final ClientController controller;

    /**
     * configName -> MqConfig
     */
    private final Map<String, MqConfig> configMap;


    public ConfigManager(final ClientController controller) {
        this.controller = controller;
        this.configMap = new ConcurrentHashMap<>(16);
    }

    @NotNull
    public MqConfig getConfig(String configName, int timeoutMillis) {
        if (configMap.containsKey(configName)) {
            MqConfig config = configMap.get(configName);
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

            MqConfig config;
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

    public void registerMqConfigs(List<MqConfig> mqConfigs) {
        for (MqConfig config : mqConfigs) {
            this.configMap.putIfAbsent(config.getName(), config);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Register MQ Config {}", config);
            }
        }
    }


}
