package com.shallowinggg.doran.client;

import org.junit.Test;

public class ClientConfigTest {

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
}
