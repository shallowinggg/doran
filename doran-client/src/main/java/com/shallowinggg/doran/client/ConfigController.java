package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ConfigController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigController.class);

    private final ConfigManager configManager;
    private final ClientApiImpl clientApiImpl;
    private ScheduledExecutorService heartBeatExecutor;
    private final ClientConfig clientConfig;

    public ConfigController(final NettyClientConfig config, final ClientConfig clientConfig) {
        this.configManager = new ConfigManager(this);
        this.clientApiImpl = new ClientApiImpl(this, config);
        this.clientConfig = clientConfig;
    }

    public void init() {
        this.heartBeatExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryImpl("heartBeat_", true));
        this.clientApiImpl.updateServerAddress(clientConfig.getServerAddr());
    }

    public void start() {
        this.heartBeatExecutor.scheduleAtFixedRate(() -> {
            try {
                this.sendHeartBeat();
            } catch (Throwable t) {
                LOGGER.error("Send heart beat fail", t);
            }
        }, 0, clientConfig.getHeartBeatServerInterval(), TimeUnit.MILLISECONDS);
    }

    private void sendHeartBeat() {
    }

    public MqConfig getMqConfig(String configName) {
        Assert.hasText(configName);
        return this.configManager.getConfig(configName, clientConfig.getTimeoutMillis());
    }


    public ClientApiImpl getClientApiImpl() {
        return clientApiImpl;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
