package com.shallowinggg.doran.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class JarDependentTest {

    @Test
    public void testMQType() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        ActiveMQConfig.DestinationType type = ActiveMQConfig.DestinationType.valueOf("TOPIC");
    }

}
