package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.MQConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shallowinggg
 */
public class ClientMetaInfo {
    private final String clientId;
    private final String clientName;
    private final Map<String, MQConfig> holdingMqConfigs;

    public ClientMetaInfo(String clientId, String clientName) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.holdingMqConfigs = new HashMap<>(16);
    }

    public void addMqConfig(MQConfig config) {
        this.holdingMqConfigs.putIfAbsent(config.getName(), config);
    }

    public boolean hasMqConfig(String configName) {
        return this.holdingMqConfigs.containsKey(configName);
    }

    public Collection<MQConfig> holdingConfigs() {
        Collection<MQConfig> mqConfigs = this.holdingMqConfigs.values();
        return Collections.unmodifiableCollection(mqConfigs);
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    @Override
    public String toString() {
        return "ClientMetaInfo{" +
                "clientId='" + clientId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", holdingMqConfigs=" + holdingMqConfigs +
                '}';
    }
}
