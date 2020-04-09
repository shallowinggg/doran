package com.shallowinggg.doran.server.transport;

import com.shallowinggg.doran.common.DataVersion;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.transport.common.RemotingUtil;
import io.netty.channel.Channel;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shallowinggg
 */
public class ClientManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientManager.class);
    private static final String CLIENT_EXPIRED_MILLIS_PROPERTY = "com.shallowinggg.doran.server.clientExpiredMillis";
    private static final long DEFAULT_CLIENT_EXPIRED_MILLIS = 1000 * 60 * 2;
    private static final long CLIENT_EXPIRED_MILLIS;
    private final Map<String, ClientMetaInfo> clientMetaInfoMap;
    private final Map<String, ClientLiveInfo> clientLiveInfoMap;

    static {
        CLIENT_EXPIRED_MILLIS = SystemPropertyUtil.getLong(CLIENT_EXPIRED_MILLIS_PROPERTY,
                DEFAULT_CLIENT_EXPIRED_MILLIS);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("-D{}: {} ", CLIENT_EXPIRED_MILLIS_PROPERTY, CLIENT_EXPIRED_MILLIS);
        }
    }

    public ClientManager(final ServerController controller) {
        this.clientMetaInfoMap = new ConcurrentHashMap<>(16);
        this.clientLiveInfoMap = new ConcurrentHashMap<>(16);
    }

    public void registerClient(ClientMetaInfo clientMetaInfo, Channel channel) {
        final String clientId = clientMetaInfo.getClientId();
        if (!clientMetaInfoMap.containsKey(clientId)) {
            clientMetaInfoMap.put(clientId, clientMetaInfo);

            ClientLiveInfo clientLiveInfo = new ClientLiveInfo();
            clientLiveInfo.setLastUpdateTimestamp(System.currentTimeMillis());
            clientLiveInfo.setDataVersion(new DataVersion());
            clientLiveInfo.setChannel(channel);
            clientLiveInfoMap.put(clientId, clientLiveInfo);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("The client {} register success", clientId);
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("The client {} has registered", clientId);
            }
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

    public void scanInactiveClient() {
        for (Map.Entry<String, ClientLiveInfo> entry : this.clientLiveInfoMap.entrySet()) {
            long lastUpdateTimestamp = entry.getValue().getLastUpdateTimestamp();
            if (lastUpdateTimestamp + CLIENT_EXPIRED_MILLIS < System.currentTimeMillis()) {
                String clientId = entry.getKey();
                Channel channel = entry.getValue().getChannel();

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("The client {} lost connection, addr: {}, expired time: {} ms",
                            clientId, channel.remoteAddress(), CLIENT_EXPIRED_MILLIS);
                }
                this.onClientDestroy(clientId, channel);
            }
        }
    }

    private void onClientDestroy(String clintId, Channel channel) {
        RemotingUtil.closeChannel(channel);
        this.clientLiveInfoMap.remove(clintId);
        this.clientMetaInfoMap.remove(clintId);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("The client {} lost connection, close its channel and remove its structure at once",
                    clintId);
        }
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
