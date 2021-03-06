package com.shallowinggg.doran.client;

import com.codahale.metrics.Counter;
import com.shallowinggg.doran.client.chooser.BuiltInProducerChooserFactory;
import com.shallowinggg.doran.client.chooser.ObjectChooser;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.MqConfigBean;
import com.shallowinggg.doran.client.common.NameGenerator;
import com.shallowinggg.doran.client.common.NameGeneratorFactory;
import com.shallowinggg.doran.client.producer.ActiveMQProducer;
import com.shallowinggg.doran.client.producer.BuiltInProducer;
import com.shallowinggg.doran.client.producer.RabbitMQProducer;
import com.shallowinggg.doran.common.ActiveMQConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class DefaultProducer implements MqConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProducer.class);

    private static final int STARTING = 1;
    private static final int RUNNING = 2;
    private static final int REBUILDING = 3;
    private static final int SHUTDOWN = 4;

    private final Counter counter;
    private final String name;
    private final ReInputEventExecutorGroup reInputExecutor;

    private volatile MQConfig config;
    private BuiltInProducer[] producers;
    private ObjectChooser<BuiltInProducer> producerChooser;
    private EventExecutorGroup sendExecutor;
    private NameGenerator nameGenerator;
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

    public DefaultProducer(String name, String configName, Counter counter) {
        Assert.hasText(name, "'name' must has text");
        Assert.hasText(configName, "'configName' must has text");
        Assert.notNull(counter, "'counter' must not be null");
        this.name = name;
        this.counter = counter;
        this.reInputExecutor = new ReInputEventExecutorGroup(1,
                new ThreadFactoryImpl(configName + "ProducerReInputExecutor_"));
        this.state = STARTING;
    }

    public void sendMessage(Message msg) {
        final int state = this.state;
        if (state == SHUTDOWN) {
            throw new IllegalStateException("Producer " + name + " has closed");
        }
        if (!isRunning(state)) {
            throw new IllegalStateException("Producer " + name + " has not initialized");
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
            throw new IllegalStateException("Producer " + name + " has closed");
        }
        if (!isRunning(state)) {
            throw new IllegalStateException("Producer " + name + " has not initialized");
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
        if(state == REBUILDING) {
            return;
        }
        this.state = REBUILDING;
        final MQConfig oldConfig = this.config;
        final int num = newConfig.getThreadNum();
        final String configName = newConfig.getName();
        BuiltInProducer[] deprecateProducers = null;

        // build new resources
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

                int deprecateNum = oldNum - num;
                deprecateProducers = new BuiltInProducer[deprecateNum];
                System.arraycopy(this.producers, num, deprecateProducers, 0, deprecateNum);
            }

            for (int i = 0; i < num; ++i) {
                newProducers[i].register(sendExecutor.next());
                newProducers[i].startResendTask();
            }
        } else {
            this.nameGenerator = NameGeneratorFactory.getInstance().producerNameGenerator(newConfig);
            for (int i = 0; i < num; ++i) {
                BuiltInProducer producer = createProducer(newConfig);
                producer.register(sendExecutor.next());
                producer.startResendTask();
                newProducers[i] = producer;
            }
            deprecateProducers = this.producers;
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
        if(deprecateProducers != null) {
            for(BuiltInProducer producer : deprecateProducers) {
                producer.close();
            }
        }

        this.sendExecutor = sendExecutor;
        this.producers = newProducers;
        this.producerChooser = producerChooser;
        this.reInputExecutor.setTransferGroup(sendExecutor);
        this.config = newConfig;
        this.state = RUNNING;

        if(LOGGER.isInfoEnabled()) {
            if(oldConfig == null) {
                LOGGER.info("Build producer {} success, config {}", name, newConfig);
            } else {
                LOGGER.info("Rebuild producer {} success, old: {}, new: {}", name, oldConfig, newConfig);
            }
        }
    }

    @Override
    public MQConfig getMqConfig() {
        return config;
    }

    public void close() {
        this.state = SHUTDOWN;
        this.reInputExecutor.shutdownGracefully();
        this.sendExecutor.shutdownGracefully();
        if(producers != null) {
            for (BuiltInProducer producer : producers) {
                producer.close();
            }
        }
    }

    @NotNull
    public Counter getCounter() {
        return this.counter;
    }

    private BuiltInProducer createProducer(final MQConfig config) {
        String name;
        switch (config.getType()) {
            case RabbitMQ:
                RabbitMQConfig rabbitMQConfig = (RabbitMQConfig) config;
                name = nameGenerator.generateName();
                return new RabbitMQProducer(name, rabbitMQConfig);
            case ActiveMQ:
                ActiveMQConfig activeMQConfig = (ActiveMQConfig) config;
                name = nameGenerator.generateName();
                return new ActiveMQProducer(name, activeMQConfig);
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Invalid config " + config);
        }
    }

    private static boolean onlyThreadNumChanged(MQConfig oldConfig, MQConfig newConfig) {
        return oldConfig.equalsIgnoreThreadNum(newConfig) &&
                oldConfig.getThreadNum() != newConfig.getThreadNum();
    }
}
