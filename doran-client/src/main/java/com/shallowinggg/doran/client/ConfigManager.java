package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager {@link MqConfig}s for one client.
 *
 * @author shallowinggg
 */
public class ConfigManager {
    private static final MqConfig NON_EXIST_CONFIG = new MqConfig();

    private final ConfigController controller;
    private final Map<String, MqConfig> configMap;


    public ConfigManager(final ConfigController controller) {
        this.controller = controller;
        this.configMap = new ConcurrentHashMap<>(16);
    }

    public MqConfig getConfig(String configName, int timeoutMillis) {
        if(configMap.containsKey(configName)) {
            MqConfig config = configMap.get(configName);
            if(config == NON_EXIST_CONFIG) {
                throw new ConfigNotExistException(configName);
            }
            return config;
        }

        // for hotspot jdk8, this is safe
        // TODO: use more compatibility way
        synchronized (configName.intern()) {
            if(configMap.containsKey(configName)) {
                return configMap.get(configName);
            }

            MqConfig config;
            try {
                config = controller.getClientApiImpl().requestConfig(configName, timeoutMillis);
                if (config != null) {
                    configMap.put(configName, config);
                }
            } catch (ConfigNotExistException e) {
                configMap.put(configName, NON_EXIST_CONFIG);
                throw e;
            }
            return config;
        }
    }




}
