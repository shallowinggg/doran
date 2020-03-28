package com.shallowinggg.doran.client;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.shallowinggg.doran.common.EmptyMQConfig;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author shallowinggg
 */
public class ClientManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientManager.class);
    private static final MQConfig NON_EXIST_CONFIG = new EmptyMQConfig();
    private static final String PRODUCER_METER_SUFFIX = ".producer";
    private static final String CONSUMER_METER_SUFFIX = ".consumer";

    private final ConcurrentMap<String, DefaultProducer> producers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DefaultConsumer> consumers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MQConfig> configs = new ConcurrentHashMap<>();

    private final MetricRegistry producerMetricRegistry;
    private final MetricRegistry consumerMetricRegistry;
    private final ClientController controller;

    public ClientManager(ClientController controller) {
        this.controller = controller;
        this.producerMetricRegistry = new MetricRegistry();
        this.consumerMetricRegistry = new MetricRegistry();
    }

    public DefaultProducer createProducer(String configName, int timeoutMillis) {
        Assert.hasText(configName);
        if (producers.containsKey(configName)) {
            return producers.get(configName);
        }
        synchronized (configName.intern()) {
            if (producers.containsKey(configName)) {
                return producers.get(configName);
            }
            final Meter meter = producerMetricRegistry.meter(configName + PRODUCER_METER_SUFFIX);
            final DefaultProducer producer = new DefaultProducer(configName, meter);
            final MQConfig config = getConfig(configName, timeoutMillis);
            producer.setMqConfig(config);
            producers.putIfAbsent(configName, producer);
            return producer;
        }
    }

    public DefaultConsumer createConsumer(String configName, int timeoutMillis) {
        Assert.hasText(configName);
        if (consumers.containsKey(configName)) {
            return consumers.get(configName);
        }
        synchronized (configName.intern()) {
            if (consumers.containsKey(configName)) {
                return consumers.get(configName);
            }
            final Meter meter = producerMetricRegistry.meter(configName + CONSUMER_METER_SUFFIX);
            final DefaultConsumer consumer = new DefaultConsumer(configName, meter);
            final MQConfig config = getConfig(configName, timeoutMillis);
            consumer.setMqConfig(config);
            consumers.put(configName, consumer);
            return consumer;
        }
    }

    @NotNull
    public MQConfig getConfig(String configName, int timeoutMillis) {
        if (configs.containsKey(configName)) {
            MQConfig config = configs.get(configName);
            if (config == NON_EXIST_CONFIG) {
                throw new ConfigNotExistException(configName);
            }
            return config;
        }

        // for hotspot jdk8, this is safe
        // TODO: use more compatibility way
        synchronized (configName.intern()) {
            if (configs.containsKey(configName)) {
                MQConfig config = configs.get(configName);
                if (config == NON_EXIST_CONFIG) {
                    throw new ConfigNotExistException(configName);
                }
                return config;
            }

            MQConfig config;
            try {
                config = controller.getClientApiImpl().requestConfig(configName, timeoutMillis);
                configs.put(configName, config);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Register MQ Config {}", config);
                }
            } catch (ConfigNotExistException e) {
                configs.put(configName, NON_EXIST_CONFIG);
                throw e;
            }
            return config;
        }
    }

    public void registerMqConfigs(List<MQConfig> mqConfigs) {
        for (MQConfig config : mqConfigs) {
            this.configs.putIfAbsent(config.getName(), config);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Register MQ Config {}", config);
            }
        }
    }
}
