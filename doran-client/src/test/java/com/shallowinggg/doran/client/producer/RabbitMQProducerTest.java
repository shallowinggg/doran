package com.shallowinggg.doran.client.producer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.shallowinggg.doran.client.DefaultProducer;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.consumer.RabbitMQConsumerTest;
import com.shallowinggg.doran.common.RabbitMQConfig;
import org.junit.Before;
import org.junit.Test;

public class RabbitMQProducerTest {
    private RabbitMQConfig config;

    @Before
    public void before() {
        config = new RabbitMQConfig();
        config.setName("test");
        config.setUri("amqp://admin:dsm123...@106.54.119.88:5672");
        config.setThreadNum(2);
        config.setTimestamp(System.currentTimeMillis());
        config.setExchangeName("exchange_demo");
        config.setQueueName("queue_demo");
        config.setRoutingKey("routingkey_demo");
    }

    @Test
    public void testStart() {
        MetricRegistry registry = new MetricRegistry();
        Counter counter = registry.counter(config.getName());
        DefaultProducer producer = new DefaultProducer("test.producer", "test", counter);
        producer.setMqConfig(config);
    }

    /**
     * @see RabbitMQConsumerTest#testReceive()
     */
    @Test
    public void testSend() {
        MetricRegistry registry = new MetricRegistry();
        Counter counter = registry.counter(config.getName());
        DefaultProducer producer = new DefaultProducer("test.producer", "test", counter);
        producer.setMqConfig(config);

        for(int i = 0; i < 100; ++i) {
            Message message = Message.createMessage("test_" + i);
            producer.sendMessage(message);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(producer.getCounter().getCount());
        producer.close();
    }

}
