package com.shallowinggg.doran.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.shallowinggg.doran.client.common.MqConfigBean;
import com.shallowinggg.doran.client.common.NameGenerator;
import com.shallowinggg.doran.client.consumer.ActiveMQConsumer;
import com.shallowinggg.doran.client.consumer.BuiltInConsumer;
import com.shallowinggg.doran.client.consumer.RabbitMQConsumer;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shallowinggg
 */
public class DefaultConsumer implements MqConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumer.class);
    private static final int NEW = 0;
    private static final int STARTING = 1;
    private static final int RUNNING = 2;
    private static final int REBUILDING = 3;
    private static final int SHUTDOWN = 4;

    private final String name;
    private final Meter meter;
    private final Counter counter;

    private MQConfig config;
    private BuiltInConsumer consumer;
    private NameGenerator nameGenerator;
    private volatile int state;

    public DefaultConsumer(String configName, Meter meter) {
        Assert.hasText(configName);
        Assert.notNull(meter, "'meter' must not be null");
        this.name = configName;
        this.meter = meter;
        this.counter = null;
        this.state = STARTING;
    }

    @Override
    public void setMqConfig(@NotNull MQConfig newConfig) {
        Assert.notNull(newConfig, "'newConfig' must not be null");
        if(state == REBUILDING) {
            return;
        }
        this.state = REBUILDING;
        final MQConfig oldConfig = this.config;
        final int num = newConfig.getThreadNum();


    }

    private BuiltInConsumer createConsumer(MQConfig config) {
        String name;
        switch (config.getType()) {
            case RabbitMQ:
                RabbitMQConfig rabbitMQConfig = (RabbitMQConfig) config;
                name = nameGenerator.generateName();
                return new RabbitMQConsumer(name, rabbitMQConfig, null, null);
            case ActiveMQ:
                ActiveMQConfig activeMQConfig = (ActiveMQConfig) config;
                name = nameGenerator.generateName();
                return new ActiveMQConsumer(name, activeMQConfig, null, null);
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Invalid config " + config);
        }
    }

    @Override
    public MQConfig getMqConfig() {
        return config;
    }

    private static boolean onlyThreadNumChanged(MQConfig oldConfig, MQConfig newConfig) {
        return oldConfig.equalsIgnoreThreadNum(newConfig) &&
                oldConfig.getThreadNum() != newConfig.getThreadNum();
    }
}
