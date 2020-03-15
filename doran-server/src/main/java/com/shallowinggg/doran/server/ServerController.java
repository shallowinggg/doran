package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.RequestCode;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingServer;
import com.shallowinggg.doran.transport.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shallowinggg
 */
public class ServerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerController.class);

    /**
     * Handle requests for all clients
     */
    private final NettyRemotingServer server;

    private final ServerConfig serverConfig;

    /**
     * Send requests to clients
     */
    private final ServerOuterApi serverOuterApi;

    /**
     * Manage client's meta configurations, e.g. id, name, mq configs holding ...
     */
    private final ClientManager clientManager;

    /**
     * Manage all mq configs
     */
    private final MqConfigManager mqConfigManager;

    public ServerController(final ServerConfig serverConfig,
                            final NettyServerConfig nettyServerConfig,
                            final NettyClientConfig nettyClientConfig) {
        this.serverConfig = serverConfig;
        this.server = new NettyRemotingServer(nettyServerConfig);
        this.serverOuterApi = new ServerOuterApi(this, nettyClientConfig);
        this.clientManager = new ClientManager(this);
        this.mqConfigManager = new MqConfigManager(this);
    }

    public void init() {
        this.registerProcessors();
    }

    private void registerProcessors() {
        ServerCoreProcessor serverCoreProcessor = new ServerCoreProcessor(this);
        this.server.registerProcessor(RequestCode.HEART_BEAT, serverCoreProcessor, null);
        this.server.registerProcessor(RequestCode.REGISTER_CLIENT, serverCoreProcessor, null);
        this.server.registerProcessor(RequestCode.REQUEST_CONFIG, serverCoreProcessor, null);
    }

    public void start() {
        this.server.start();
    }

    public ClientManager getClientManager() {
        return clientManager;
    }

    public MqConfigManager getMqConfigManager() {
        return mqConfigManager;
    }
}
