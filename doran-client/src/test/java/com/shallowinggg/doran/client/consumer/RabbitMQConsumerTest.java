package com.shallowinggg.doran.client.consumer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.shallowinggg.doran.client.DefaultConsumer;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.producer.RabbitMQProducerTest;
import com.shallowinggg.doran.common.RabbitMQConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class RabbitMQConsumerTest {

    private RabbitMQConfig config;

    @Before
    public void before() {
        config = new RabbitMQConfig();
        config.setName("consumer_test");
        config.setUri("amqp://admin:dsm123...@106.54.119.88:5672");
        config.setThreadNum(2);
        config.setTimestamp(System.currentTimeMillis());
        config.setQueueName("queue_demo");
    }

    /**
     * @see RabbitMQProducerTest#testSend()
     */
    @Test
    public void testReceive() {
        MetricRegistry registry = new MetricRegistry();
        Counter counter = registry.counter(config.getName());
        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message) {
                // debug
            }

            @Override
            public boolean accept(Message message) {
                return true;
            }
        };

        DefaultConsumer consumer = new DefaultConsumer("test.consumer", counter, Collections.singletonList(listener));
        consumer.setMqConfig(config);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(consumer.getCounter().getCount());
        consumer.close();
    }
}
