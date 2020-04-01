package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.shallowinggg.doran.client.DefaultProducer;
import com.shallowinggg.doran.common.RabbitMQConfig;
import org.junit.Test;

public class RabbitMQProducerTest {

    @Test
    public void testStart() {
        RabbitMQConfig config = new RabbitMQConfig();
        config.setName("test");
        config.setUri("amqp://admin:dsm123...@106.54.119.88:5672");
        config.setThreadNum(2);
        config.setTimestamp(System.currentTimeMillis());
        config.setExchangeName("exchange_demo");
        config.setQueueName("queue_demo");
        config.setRoutingKey("routingkey_demo");

        MetricRegistry registry = new MetricRegistry();
        Counter counter = registry.counter(config.getName());
        DefaultProducer producer = new DefaultProducer("test.producer", "test", counter);
        producer.setMqConfig(config);
    }

}
