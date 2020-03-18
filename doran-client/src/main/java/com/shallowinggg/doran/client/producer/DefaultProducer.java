package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.shallowinggg.doran.client.MqConfigBean;
import com.shallowinggg.doran.client.chooser.ObjectChooser;
import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.util.Assert;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class DefaultProducer implements MqConfigBean {
    private static final int NEW = 0;
    private static final int STARTING = 1;
    private static final int RUNNING = 2;
    private static final int REBUILDING = 3;
    private static final int SHUTDOWN = 4;

    private volatile MqConfig config;
    private final Counter counter;
    private BuiltInProducer[] producers;
    private ObjectChooser<BuiltInProducer> producerChooser;
    private DefaultEventExecutorGroup sendExecutor;
    private volatile int state = NEW;

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean isRunning(int c) {
        return c > STARTING && c < SHUTDOWN;
    }

    public DefaultProducer(Counter counter) {
        this.counter = counter;
        this.state = STARTING;
    }

    public DefaultProducer(Counter counter, MqConfig config) {
        this.counter = counter;
        this.config = config;

        this.state = STARTING;
    }

    public Future<String> sendMessage(byte[] msg) {
        if(!isRunning(state)) {

        }
        counter.inc();
        return null;
    }

    public Future<String> sendMessage(byte[] msg, long delay, TimeUnit unit) {
        counter.inc();
        return null;
    }

    @Override
    public void setMqConfig(@NotNull MqConfig newConfig) {
        Assert.notNull(newConfig, "'newConfig' must not be null");
        this.config = newConfig;
        this.state = RUNNING;
    }

    @Override
    public MqConfig getMqConfig() {
        return config;
    }
}
