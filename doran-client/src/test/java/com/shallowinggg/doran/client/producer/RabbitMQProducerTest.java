package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.shallowinggg.doran.client.DefaultProducer;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.common.RabbitMQConfig;
import org.junit.Test;

public class RabbitMQProducerTest {

    @Test
    public void testStart() {
        RabbitMQConfig config = new RabbitMQConfig("test",
                MQType.RabbitMQ,
                "amqp://admin:dsm123...@106.54.119.88:5672",
                null,
                null,
                2,
                System.currentTimeMillis(),
                "exchange_demo",
                "queue_demo",
                "routingkey_demo");
        MetricRegistry registry = new MetricRegistry();
        Counter counter = registry.counter(config.getName());
        DefaultProducer producer = new DefaultProducer("test.producer", "test", counter);
        producer.setMqConfig(config);
    }

}
