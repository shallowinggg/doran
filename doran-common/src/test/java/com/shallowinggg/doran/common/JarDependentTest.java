package com.shallowinggg.doran.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class JarDependentTest {

    @Test
    public void testMQType() throws JsonProcessingException {
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
        System.out.println(mapper.writeValueAsString(mqConfig));
    }

}
