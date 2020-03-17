package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.shallowinggg.doran.client.chooser.ObjectChooser;
import com.shallowinggg.doran.common.MqConfig;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class DefaultProducer {
    private final String configName;
    private final MqConfig config;
    private final Counter counter;
    private BuiltInProducer[] producers;
    private ObjectChooser<BuiltInProducer> producerChooser;
    private DefaultEventExecutorGroup sendGroup;

    public DefaultProducer(String configName, Counter counter, MqConfig config) {
        this.configName = configName;
        this.counter = counter;
        this.config = config;
    }

    public Future<String> sendMessage(byte[] msg) {
        counter.inc();
        return null;
    }

    public Future<String> sendMessage(byte[] msg, long delay, TimeUnit unit) {
        counter.inc();
        return null;
    }
}
