package com.shallowinggg.doran.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.concurrent.*;

/**
 * @author shallowinggg
 */
public class ClientController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientController.class);

    private final ConfigManager configManager;
    private final ClientApiImpl clientApiImpl;
    private final MetricRegistry producerMetricRegistry;
    private final MetricRegistry consumerMetricRegistry;
    private ScheduledExecutorService heartBeatExecutor;
    private final ClientConfig clientConfig;

    /**
     * This executor is provided to handle async tasks,
     * like register client, request MQ config etc.
     * <p>
     * Most of this tasks are I/O intensive, so its maximum
     * pool size should be a little big.
     */
    private ExecutorService asyncExecutor;

    public ClientController(final NettyClientConfig config, final ClientConfig clientConfig) {
        this.configManager = new ConfigManager(this);
        this.clientApiImpl = new ClientApiImpl(this, config);
        this.producerMetricRegistry = new MetricRegistry();
        this.consumerMetricRegistry = new MetricRegistry();
        this.clientConfig = clientConfig;
    }

    public void init() {
        this.clientApiImpl.init();
        this.clientApiImpl.updateServerAddress(clientConfig.getServerAddr());
        this.heartBeatExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryImpl("heartBeat_", true));
        this.asyncExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                20, 2, TimeUnit.MINUTES, new LinkedBlockingQueue<>(100),
                new ThreadFactoryImpl("clientAsync_"));
    }

    public void start() {
        this.clientApiImpl.start();
        this.asyncExecutor.execute(this::registerClient);
        this.heartBeatExecutor.scheduleAtFixedRate(() -> {
            try {
                this.sendHeartBeat();
            } catch (Throwable t) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Send heartbeat fail", t);
                }
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
        SortedMap<String, Counter> producerCounters = producerMetricRegistry.getCounters();
        SortedMap<String, Counter> consumerCounters = consumerMetricRegistry.getCounters();

    }

    public MQConfig getMqConfig(String configName) {
        Assert.hasText(configName);
        return this.configManager.getConfig(configName, clientConfig.getTimeoutMillis());
    }

    public void shutdown() {
        this.clientApiImpl.shutdown();
        this.heartBeatExecutor.shutdown();
        this.asyncExecutor.shutdown();
    }

    public DefaultProducer createProducer(String configName) {
        return createProducer(configName, true);
    }

    public DefaultProducer createProducer(String configName, boolean async) {
        Assert.hasText(configName);
        synchronized (configName.intern()) {
            final Counter messageCounter = producerMetricRegistry.counter(configName);
            final DefaultProducer producer = new DefaultProducer(configName, messageCounter);
            if (async) {
                asyncExecutor.submit(() -> {
                    MQConfig config = getMqConfig(configName);
                    producer.setMqConfig(config);
                });
            } else {
                final MQConfig config = getMqConfig(configName);
                producer.setMqConfig(config);
            }

            return producer;
        }
    }

    @NotNull
    public ClientApiImpl getClientApiImpl() {
        return clientApiImpl;
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
