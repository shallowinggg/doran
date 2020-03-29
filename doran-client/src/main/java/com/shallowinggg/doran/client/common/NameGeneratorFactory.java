package com.shallowinggg.doran.client.common;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shallowinggg
 */
public class NameGeneratorFactory {
    private static final NameGeneratorFactory INSTANCE = new NameGeneratorFactory();

    private NameGeneratorFactory() {
    }

    public static NameGeneratorFactory getInstance() {
        return INSTANCE;
    }

    public NameGenerator producerNameGenerator(@NotNull MQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        String name = config.getName();
        switch (config.getType()) {
            case RabbitMQ:
                return new RabbitMQProducerNameGenerator(name);
            case ActiveMQ:
                return new ActiveMQProducerNameGenerator(name);
            default:
                throw new IllegalArgumentException("Invalid MQ config " + config);
        }
    }

    public NameGenerator consumerNameGenerator(@NotNull MQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        String name = config.getName();
        switch (config.getType()) {
            case RabbitMQ:
                return new RabbitMQConsumerNameGenerator(name);
            case ActiveMQ:
                return new ActiveMQConsuemrNameGenerator(name);
            default:
                throw new IllegalArgumentException("Invalid MQ config " + config);
        }
    }

    @Immutable
    private static class RabbitMQProducerNameGenerator implements NameGenerator {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger();

        RabbitMQProducerNameGenerator(String configName) {
            this.prefix = "rabbitmq-producer-" + configName + "-";
        }

        @Override
        public String generateName() {
            return prefix + Math.abs(idx.incrementAndGet());
        }
    }

    @Immutable
    private static class ActiveMQProducerNameGenerator implements NameGenerator {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger();

        ActiveMQProducerNameGenerator(String configName) {
            this.prefix = "activemq-producer-" + configName + "-";
        }

        @Override
        public String generateName() {
            return prefix + Math.abs(idx.incrementAndGet());
        }
    }

    @Immutable
    private static class RabbitMQConsumerNameGenerator implements NameGenerator {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger();

        RabbitMQConsumerNameGenerator(String configName) {
            this.prefix = "rabbitmq-consumer-" + configName + "-";
        }

        @Override
        public String generateName() {
            return prefix + Math.abs(idx.incrementAndGet());
        }
    }

    @Immutable
    private static class ActiveMQConsuemrNameGenerator implements NameGenerator {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger();

        ActiveMQConsuemrNameGenerator(String configName) {
            this.prefix = "activemq-consumer-" + configName + "-";
        }

        @Override
        public String generateName() {
            return prefix + Math.abs(idx.incrementAndGet());
        }
    }
}
