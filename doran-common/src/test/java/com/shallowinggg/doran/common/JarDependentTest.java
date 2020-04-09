package com.shallowinggg.doran.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

public class JarDependentTest {

    @Test
    public void testMQType() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        RabbitMQConfig config = new RabbitMQConfig();
        config.setName("test");
        config.setUri("amqp://admin:dsm123...@106.54.119.88:5672");
        config.setThreadNum(2);
        config.setTimestamp(System.currentTimeMillis());
        config.setExchangeName("exchange_demo");
        config.setQueueName("queue_demo");
        config.setRoutingKey("routingkey_demo");

        MQConfig mqConfig = config;
        String json = mapper.writeValueAsString(mqConfig);
        System.out.println(json);

        RabbitMQConfig des = mapper.readValue(json, RabbitMQConfig.class);
        System.out.println(des);
    }

}
