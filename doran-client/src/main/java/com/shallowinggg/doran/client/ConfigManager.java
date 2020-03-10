package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.RequestMqConfigRequestHeader;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.transport.RemotingClient;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manager {@link MqConfig}s for one client.
 *
 * @author shallowinggg
 */
public class ConfigManager {
    private final ConfigController controller;
    private final Map<String, MqConfig> configMap;



    ConfigManager(final ConfigController controller) {
        this.controller = controller;
        this.configMap = new ConcurrentHashMap<>(16);
    }

    public MqConfig getConfig(String topicName) {
        if(configMap.containsKey(topicName)) {
            return configMap.get(topicName);
        }
        return null;
    }




}
