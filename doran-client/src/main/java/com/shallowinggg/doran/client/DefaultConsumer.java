package com.shallowinggg.doran.client;

import com.codahale.metrics.Counter;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.MqConfigBean;
import com.shallowinggg.doran.client.common.NameGeneratorFactory;
import com.shallowinggg.doran.client.consumer.ActiveMQConsumer;
import com.shallowinggg.doran.client.consumer.BuiltInConsumer;
import com.shallowinggg.doran.client.consumer.MessageListener;
import com.shallowinggg.doran.client.consumer.RabbitMQConsumer;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class DefaultConsumer implements MqConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumer.class);
    private static final int BLOCKING_QUEUE_SIZE = SystemPropertyUtil.getInt("com.shallowinggg.consumer.blockingQueueSize", 256);

    private static final int STARTING = 1;
    private static final int RUNNING = 2;
    private static final int REBUILDING = 3;
    private static final int SHUTDOWN = 4;

    private final String name;
    private final Counter counter;
    private final List<MessageListener> listeners;

    private MQConfig config;
    private BuiltInConsumer consumer;
    private ThreadPoolExecutor executor;
    private volatile int state;

    public DefaultConsumer(String name, Counter counter) {
        Assert.hasText(name, "'name' must has text");
        Assert.notNull(counter, "'counter' must not be null");
        this.name = name;
        this.counter = counter;
        this.listeners = null;
        this.state = STARTING;
    }

    public DefaultConsumer(String name, Counter counter, List<MessageListener> listeners) {
        Assert.hasText(name, "'name' must has text");
        Assert.notNull(counter, "'counter' must not be null");
        Assert.isTrue(CollectionUtils.isNotEmpty(listeners), "'listeners' must not be empty");

        this.name = name;
        this.counter = counter;
        List<MessageListener> temp = new ArrayList<>(listeners.size() + 1);
        Collections.copy(temp, listeners);
        temp.add(new MetricMessageListener(counter));
        this.listeners = temp;
        this.state = STARTING;
    }

    public Message receive() {
        Message message = consumer.receive();
        counter.inc();
        return message;
    }

    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        Message message = consumer.receive(timeout, unit);
        counter.inc();
        return message;
    }

    @Override
    public void setMqConfig(@NotNull MQConfig newConfig) {
        Assert.notNull(newConfig, "'newConfig' must not be null");
        if (state == REBUILDING) {
            return;
        }
        this.state = REBUILDING;
        final MQConfig oldConfig = this.config;
        String configName = newConfig.getName();
        final int num = newConfig.getThreadNum();

        if (oldConfig != null && onlyThreadNumChanged(oldConfig, newConfig)) {
            executor.setCorePoolSize(num);
            executor.setMaximumPoolSize(num);
        } else {
            executor = new ThreadPoolExecutor(num, num, 0, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(BLOCKING_QUEUE_SIZE), new ThreadFactoryImpl(configName + "-consumer-"));
            consumer = createConsumer(newConfig);
        }

        this.config = newConfig;
        this.state = RUNNING;

        if (LOGGER.isInfoEnabled()) {
            if (oldConfig == null) {
                LOGGER.info("Build consumer {} success, config {}", name, newConfig);
            } else {
                LOGGER.info("Rebuild consumer {} success, old: {}, new: {}", name, oldConfig, newConfig);
            }
        }

    }

    public void close() {
        this.state = SHUTDOWN;

    }

    private BuiltInConsumer createConsumer(MQConfig config) {
        String name;
        switch (config.getType()) {
            case RabbitMQ:
                RabbitMQConfig rabbitMQConfig = (RabbitMQConfig) config;
                name = NameGeneratorFactory.getInstance().consumerNameGenerator(config).generateName();
                return new RabbitMQConsumer(name, rabbitMQConfig, executor, listeners);
            case ActiveMQ:
                ActiveMQConfig activeMQConfig = (ActiveMQConfig) config;
                name = NameGeneratorFactory.getInstance().consumerNameGenerator(config).generateName();
                return new ActiveMQConsumer(name, activeMQConfig, executor, listeners);
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Invalid config: " + config);
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

    private static class MetricMessageListener implements MessageListener {
        private final Counter counter;

        MetricMessageListener(Counter counter) {
            this.counter = counter;
        }

        @Override
        public void onMessage(Message message) {
            counter.inc();
        }

        @Override
        public boolean accept(Message message) {
            return true;
        }
    }
}
