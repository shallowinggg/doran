package com.shallowinggg.doran.server.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.common.RabbitMQConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class Redis {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void test() throws JsonProcessingException {
        String key = "test";
        MQType type = MQType.RabbitMQ;

        RabbitMQConfig config = new RabbitMQConfig();
        config.setName("test");
        config.setUri("amqp://admin:dsm123...@106.54.119.88:5672");
        config.setThreadNum(2);
        config.setTimestamp(System.currentTimeMillis());
        config.setExchangeName("exchange_demo");
        config.setQueueName("queue_demo");
        config.setRoutingKey("routingkey_demo");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(config);
        redisTemplate.opsForHash().putIfAbsent(key, type.name(), json);
        System.out.println(redisTemplate.opsForHash().get(key, type.name()));
    }
}
