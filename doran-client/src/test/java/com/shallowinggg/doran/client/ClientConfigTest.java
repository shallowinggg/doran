package com.shallowinggg.doran.client;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfigTest.class);

    @Test
    public void testClientId() {
        ClientConfig clientConfig = new ClientConfig();
        System.out.println(clientConfig.getClientId());
    }

    @Test
    public void testClientName() {
        ClientConfig clientConfig = new ClientConfig();
        System.out.println(clientConfig.getClientName());
    }

    @Test
    public void testLog() {
        try {
            throw new RuntimeException("error message");
        } catch (RuntimeException e) {
            if(LOGGER.isErrorEnabled()) {
                LOGGER.error("fail, cause: {}", e.getMessage(), e);
            }
        }
    }
}
