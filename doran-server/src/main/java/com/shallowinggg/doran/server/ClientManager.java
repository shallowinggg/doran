package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shallowinggg
 */
public class ClientManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientManager.class);
    private final ServerController controller;
    private final Map<String, ClientMetaInfo> clientMetaInfoMap;

    public ClientManager(final ServerController controller) {
        this.controller = controller;
        this.clientMetaInfoMap = new ConcurrentHashMap<>(16);
    }

    public void registerClient(ClientMetaInfo clientMetaInfo) {
        final String clientId = clientMetaInfo.getClientId();
        if(!clientMetaInfoMap.containsKey(clientId)) {
            clientMetaInfoMap.put(clientId, clientMetaInfo);
            LOGGER.info("client {} register success", clientId);
        } else {
            LOGGER.warn("client {} has registered", clientId);
        }
    }

    public boolean hasClient(String clientId) {
        Assert.hasText(clientId);
        return this.clientMetaInfoMap.containsKey(clientId);
    }

    public ClientMetaInfo getClientMetaInfo(String clientId) {
        Assert.hasText(clientId);
        return this.clientMetaInfoMap.get(clientId);
    }
}
