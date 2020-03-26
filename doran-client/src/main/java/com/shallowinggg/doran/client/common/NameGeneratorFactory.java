package com.shallowinggg.doran.client.common;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;

/**
 * @author shallowinggg
 */
public class NameGeneratorFactory {
    private static final NameGeneratorFactory INSTANCE = new NameGeneratorFactory();

    private NameGeneratorFactory() {}

    public static NameGeneratorFactory getInstance() {
        return INSTANCE;
    }

    public String generateProducerName(@NotNull MQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        switch (config.getType()) {
            case RabbitMQ:
            case ActiveMQ:
            default:
                return null;
        }
    }

    public String generateConsumerName(@NotNull MQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        switch (config.getType()) {
            case RabbitMQ:
            case ActiveMQ:
            default:
                return null;
        }
    }

    private static class RabbitMQProducerNameGenerator implements NameGenerator {
        private static final String prefix = "rabbitmq-producer-";

        @Override
        public String generateName(@NotNull MQConfig config) {
            return null;
        }
    }
}
