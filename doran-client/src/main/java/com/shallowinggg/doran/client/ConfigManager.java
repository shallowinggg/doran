package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager {@link MqConfig}s for one client.
 *
 * @author shallowinggg
 */
public class ConfigManager {
    private final ConfigController controller;
    private final Map<String, MqConfig> configMap;


    public ConfigManager(final ConfigController controller) {
        this.controller = controller;
        this.configMap = new ConcurrentHashMap<>(16);
    }

    public MqConfig getConfig(String configName) {
        if(configMap.containsKey(configName)) {
            return configMap.get(configName);
        }

        // 并发控制，避免多个线程同时请求同一个配置
        MqConfig config = controller.getClientApiImpl().requestConfig(configName);
        if(config != null) {
            configMap.put(configName, config);
        }
        return config;
    }




}
