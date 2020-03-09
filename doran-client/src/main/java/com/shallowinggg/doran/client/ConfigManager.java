package com.shallowinggg.doran.client;

import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager {@link MqConfig}s for one client.
 *
 * @author shallowinggg
 */
public class ConfigManager {
    private final Map<String, MqConfig> configMap;

    private final NettyRemotingClient client;
    private final ExecutorService clientService;

    private ConfigManager() {
        NettyClientConfig config = new NettyClientConfig();
        this.client = new NettyRemotingClient(config);
        this.configMap = new HashMap<>(16);
        this.clientService = Executors.newFixedThreadPool(2);
    }


}
