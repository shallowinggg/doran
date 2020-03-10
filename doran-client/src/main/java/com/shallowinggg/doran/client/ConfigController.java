package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.transport.RemotingClient;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ConfigController {

    private final ConfigManager configManager;
    private final ClientApiImpl clientApiImpl;

    public ConfigController(final NettyClientConfig config, final ClientConfig clientConfig) {
        this.configManager = new ConfigManager(this);

        ClientManageProcessor processor = new ClientManageProcessor(configManager);
        this.clientApiImpl = new ClientApiImpl(this, config, processor, clientConfig);
    }
}
