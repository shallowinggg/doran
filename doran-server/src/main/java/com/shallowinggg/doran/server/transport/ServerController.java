package com.shallowinggg.doran.server.transport;

import com.shallowinggg.doran.common.RequestCode;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.server.web.service.ClientService;
import com.shallowinggg.doran.server.web.service.MQConfigService;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingServer;
import com.shallowinggg.doran.transport.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
@Component
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
    private final MQConfigManager mqConfigManager;

    private final ScheduledExecutorService scheduledExecutorService;

    public ServerController(final ServerConfig serverConfig,
                            final NettyServerConfig nettyServerConfig,
                            final NettyClientConfig nettyClientConfig,
                            final MQConfigService mqConfigService,
                            final ClientService clientService) {
        this.serverConfig = serverConfig;
        this.server = new NettyRemotingServer(nettyServerConfig);
        this.serverOuterApi = new ServerOuterApi(this, nettyClientConfig);
        this.clientManager = new ClientManager(this);
        this.mqConfigManager = new MQConfigManager();
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryImpl("serverScheduledThread_"));
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
        this.scheduledExecutorService.scheduleAtFixedRate(this.clientManager::scanInactiveClient,
                5, 15, TimeUnit.SECONDS);
    }

    public ClientManager getClientManager() {
        return clientManager;
    }

    public MQConfigManager getMqConfigManager() {
        return mqConfigManager;
    }
}
