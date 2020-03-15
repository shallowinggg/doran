package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.DataVersion;
import com.shallowinggg.doran.common.util.Assert;
import io.netty.channel.Channel;
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
    private final Map<String, ClientLiveInfo> clientLiveInfoMap;

    public ClientManager(final ServerController controller) {
        this.controller = controller;
        this.clientMetaInfoMap = new ConcurrentHashMap<>(16);
        this.clientLiveInfoMap = new ConcurrentHashMap<>(16);
    }

    public void registerClient(ClientMetaInfo clientMetaInfo) {
        final String clientId = clientMetaInfo.getClientId();
        if(!clientMetaInfoMap.containsKey(clientId)) {
            clientMetaInfoMap.put(clientId, clientMetaInfo);
            // TODO: record it to clientLiveInfoMap,
            // this may modify this method's declaration.
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

    /**
     * This class is provided to store client heartbeat information.
     */
    private static class ClientLiveInfo {
        /**
         * Last time that the client sent heartbeat
         */
        private long lastUpdateTimestamp;
        private DataVersion dataVersion;

        /**
         * Netty channel that the client uses
         */
        private Channel channel;

        public long getLastUpdateTimestamp() {
            return lastUpdateTimestamp;
        }

        public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
            this.lastUpdateTimestamp = lastUpdateTimestamp;
        }

        public DataVersion getDataVersion() {
            return dataVersion;
        }

        public void setDataVersion(DataVersion dataVersion) {
            this.dataVersion = dataVersion;
        }

        public Channel getChannel() {
            return channel;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        @Override
        public String toString() {
            return "ClientLiveInfo{" +
                    "lastUpdateTimestamp=" + lastUpdateTimestamp +
                    ", dataVersion=" + dataVersion +
                    ", channel=" + channel +
                    '}';
        }
    }
}
