package com.shallowinggg.doran.client;

import com.codahale.metrics.MetricRegistry;
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
public class ClientController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientController.class);

    private final ConfigManager configManager;
    private final ClientApiImpl clientApiImpl;
    private final MetricRegistry metricRegistry;
    private ScheduledExecutorService heartBeatExecutor;
    private final ClientConfig clientConfig;

    public ClientController(final NettyClientConfig config, final ClientConfig clientConfig) {
        this.configManager = new ConfigManager(this);
        this.clientApiImpl = new ClientApiImpl(this, config);
        this.metricRegistry = new MetricRegistry();
        this.clientConfig = clientConfig;
    }

    public void init() {
        this.heartBeatExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryImpl("heartBeat_", true));
        this.clientApiImpl.updateServerAddress(clientConfig.getServerAddr());
    }

    public void start() {
        this.registerClient();
        this.heartBeatExecutor.scheduleAtFixedRate(() -> {
            try {
                this.sendHeartBeat();
            } catch (Throwable t) {
                LOGGER.error("Send heart beat fail", t);
            }
        }, 10 * 1000, clientConfig.getHeartBeatServerInterval(), TimeUnit.MILLISECONDS);
    }

    private void registerClient() {
        this.clientApiImpl.registerClient(clientConfig.getClientId(),
                clientConfig.getClientName(),
                clientConfig.getTimeoutMillis());
    }

    private void sendHeartBeat() {
        // 发送已开启的生产者，消费者信息，以及其发送消费消息的数量
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
