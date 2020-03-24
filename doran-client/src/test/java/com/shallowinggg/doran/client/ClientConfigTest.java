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
        Runnable r = () -> System.out.println("111");

    }
}
