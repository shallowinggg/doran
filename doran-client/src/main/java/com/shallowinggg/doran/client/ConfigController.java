package com.shallowinggg.doran.client;

import com.shallowinggg.doran.transport.netty.NettyClientConfig;

/**
 * @author shallowinggg
 */
public class ConfigController {

    private final ConfigManager configManager;
    private final ClientApiImpl clientApiImpl;

    public ConfigController(final NettyClientConfig config, final ClientConfig clientConfig) {
        this.configManager = new ConfigManager(this);

        ClientManageProcessor processor = new ClientManageProcessor(configManager);
        this.clientApiImpl = new ClientApiImpl(config, processor, clientConfig);
    }


    public ClientApiImpl getClientApiImpl() {
        return clientApiImpl;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
