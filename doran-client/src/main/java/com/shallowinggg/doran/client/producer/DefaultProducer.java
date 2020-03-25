package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.shallowinggg.doran.client.Message;
import com.shallowinggg.doran.client.MqConfigBean;
import com.shallowinggg.doran.client.chooser.BuiltInProducerChooserFactory;
import com.shallowinggg.doran.client.chooser.ObjectChooser;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.RabbitMQConfig;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.concurrent.DoranEventExecutor;
import com.shallowinggg.doran.common.util.concurrent.DoranEventExecutorGroup;
import com.shallowinggg.doran.common.util.concurrent.ReInputEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
    private volatile int state;

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean isRunning(int c) {
        return c == RUNNING || c == REBUILDING;
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

    public void sendMessage(Message msg) {
        final int state = this.state;
        if (state == SHUTDOWN) {
            throw new IllegalStateException("Producer has been closed");
        }
        if (!isRunning(state)) {
            throw new IllegalStateException("Producer has not initialized");
        }
        if (isRebuilding(state)) {
            reInputExecutor.submit(() -> {
                BuiltInProducer producer = producerChooser.next();
                EventExecutor executor = producer.executor();
                if (executor.inEventLoop()) {
                    producer.sendMessage(msg);
                } else {
                    executor.submit(() -> producer.sendMessage(msg));
                }
            });
        } else {
            BuiltInProducer producer = producerChooser.next();
            producer.executor().submit(() -> producer.sendMessage(msg));
        }
        counter.inc();
    }

    public void sendMessage(Message msg, long delay, TimeUnit unit) {
        final int state = this.state;
        if (state == SHUTDOWN) {
            throw new IllegalStateException("Producer has been closed");
        }
        if (!isRunning(state)) {
            throw new IllegalStateException("Producer has not initialized");
        }
        if (isRebuilding(state)) {
            reInputExecutor.submit(() -> {
                BuiltInProducer producer = producerChooser.next();
                EventExecutor executor = producer.executor();
                if (executor.inEventLoop()) {
                    producer.sendMessage(msg, delay, unit);
                } else {
                    executor.submit(() -> producer.sendMessage(msg, delay, unit));
                }
            });
        } else {
            BuiltInProducer producer = producerChooser.next();
            producer.executor().submit(() -> producer.sendMessage(msg));
        }
        counter.inc();
    }

    @Override
    public void setMqConfig(@NotNull MQConfig newConfig) {
        Assert.notNull(newConfig, "'newConfig' must not be null");
        this.state = REBUILDING;
        final MQConfig oldConfig = this.config;
        final int num = newConfig.getThreadNum();
        final String configName = newConfig.getName();

        EventExecutorGroup sendExecutor = new DoranEventExecutorGroup(num,
                new ThreadFactoryImpl(configName + "ProducerExecutor_"));
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
                newProducers[i].startResendTask();
            }
        } else {
            for (int i = 0; i < num; ++i) {
                BuiltInProducer producer = createProducer(newConfig);
                producer.register(sendExecutor.next());
                producer.startResendTask();
                newProducers[i] = producer;
            }
        }
        producerChooser = BuiltInProducerChooserFactory.INSTANCE.newChooser(newProducers);

        // transfer old uncompleted tasks to new executor
        if (this.sendExecutor != null) {
            for (EventExecutor executor : this.sendExecutor) {
                DoranEventExecutor doranEventExecutor = (DoranEventExecutor) executor;
                List<Runnable> tasks = doranEventExecutor.drainQueue();
                for (Runnable task : tasks) {
                    sendExecutor.execute(task);
                }
            }
            this.sendExecutor.shutdownGracefully();
        }

        this.sendExecutor = sendExecutor;
        this.producers = newProducers;
        this.producerChooser = producerChooser;
        this.reInputExecutor.setTransferGroup(sendExecutor);
        this.config = newConfig;
        this.state = RUNNING;
    }

    @Override
    public MQConfig getMqConfig() {
        return config;
    }

    public void close() {
        this.state = SHUTDOWN;
        this.reInputExecutor.shutdownGracefully();
        this.sendExecutor.shutdownGracefully();
    }

    private BuiltInProducer createProducer(final MQConfig config) {
        switch (config.getType()) {
            case RabbitMQ:
                RabbitMQConfig rabbitMQConfig = (RabbitMQConfig) config;
                return new RabbitMQProducer(rabbitMQConfig);
            case ActiveMQ:
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("");
        }
    }

    private static boolean onlyThreadNumChanged(MQConfig oldConfig, MQConfig newConfig) {
        return oldConfig.equalsIgnoreThreadNum(newConfig) &&
                oldConfig.getThreadNum() != newConfig.getThreadNum();
    }
}
