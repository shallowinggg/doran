package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.client.MqConfigBean;
import com.shallowinggg.doran.client.chooser.BuiltInProducerChooserFactory;
import com.shallowinggg.doran.client.chooser.ObjectChooser;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.JarDependent;
import com.shallowinggg.doran.common.util.concurrent.DoranEventExecutor;
import com.shallowinggg.doran.common.util.concurrent.DoranEventExecutorGroup;
import com.shallowinggg.doran.common.util.concurrent.ReInputEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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

    private volatile MQConfig config;
    private final Counter counter;
    private BuiltInProducer[] producers;
    private ObjectChooser<BuiltInProducer> producerChooser;
    private EventExecutorGroup sendExecutor;
    private final ReInputEventExecutorGroup reInputExecutor;
    private volatile int state = NEW;

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean isRunning(int c) {
        return c > STARTING && c < SHUTDOWN;
    }

    private static boolean isRebuilding(int c) {
        return c == REBUILDING;
    }

    public DefaultProducer(String configName, Counter counter) {
        this.counter = counter;
        this.reInputExecutor = new ReInputEventExecutorGroup(1,
                new ThreadFactoryImpl(configName + "ProducerReInputExecutor_"));
        this.state = STARTING;
    }

    public Future<String> sendMessage(Message msg) {
        final int state = this.state;
        if (!isRunning(state)) {
            throw new IllegalStateException("producer has not initialized");
        }
        if(isRebuilding(state)) {
            // send message to reInput executor
        }
        // send message to send executor and use chooser to select producer
        counter.inc();
        return null;
    }

    public Future<String> sendMessage(byte[] msg, long delay, TimeUnit unit) {
        counter.inc();
        return null;
    }

    @Override
    public void setMqConfig(@NotNull MQConfig newConfig) {
        Assert.notNull(newConfig, "'newConfig' must not be null");
        this.state = REBUILDING;
        final MQConfig oldConfig = this.config;
        final int num = newConfig.getThreadNum();
        final String configName = newConfig.getName();

        EventExecutorGroup sendExecutor = new DoranEventExecutorGroup(num,
                new ThreadFactoryImpl(configName + "SendExecutor_"));
        BuiltInProducer[] newProducers = new BuiltInProducer[num];
        ObjectChooser<BuiltInProducer> producerChooser;
        if (oldConfig != null && onlyThreadNumChanged(oldConfig, newConfig)) {
            final int oldNum = oldConfig.getThreadNum();
            // enlarge
            if (oldNum < num) {
                System.arraycopy(this.producers, 0, newProducers, 0, oldNum);
                for (int i = oldNum; i < num; ++i) {
                    newProducers[i] = createProducer(newConfig);
                }
            } else {
                System.arraycopy(this.producers, 0, newProducers, 0, num);
            }

            for (int i = 0; i < num; ++i) {
                newProducers[i].register(sendExecutor.next());
            }
        } else {
            for (int i = 0; i < num; ++i) {
                BuiltInProducer producer = createProducer(newConfig);
                producer.register(sendExecutor.next());
                newProducers[i] = producer;
            }
        }
        producerChooser = BuiltInProducerChooserFactory.INSTANCE.newChooser(newProducers);

        if(this.sendExecutor != null) {
            for(EventExecutor executor : this.sendExecutor) {
                DoranEventExecutor doranEventExecutor = (DoranEventExecutor) executor;
                List<Runnable> tasks = doranEventExecutor.drainQueue();
                for(Runnable task : tasks) {
                    sendExecutor.execute(task);
                }
            }
        }

        this.sendExecutor = sendExecutor;
        this.producers = newProducers;
        this.producerChooser = producerChooser;
        this.reInputExecutor.next().setTransferGroup(sendExecutor);
        this.config = newConfig;
        this.state = RUNNING;
    }

    @Override
    public MQConfig getMqConfig() {
        return config;
    }

    private BuiltInProducer createProducer(final MQConfig config) {
        switch (JarDependent.mqType()) {
            case RabbitMQ:
                return new RabbitMQProducer(config);
            case ActiveMQ:
            case UNKNOWN:
            default:
                throw new IllegalStateException("");
        }
    }

    private static boolean onlyThreadNumChanged(MQConfig oldConfig, MQConfig newConfig) {
        return oldConfig.equalsIgnoreThreadNum(newConfig) &&
                oldConfig.getThreadNum() != newConfig.getThreadNum();
    }
}
