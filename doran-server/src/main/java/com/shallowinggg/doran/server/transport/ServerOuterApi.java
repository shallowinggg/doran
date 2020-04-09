package com.shallowinggg.doran.server.transport;

import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;

/**
 * @author shallowinggg
 */
public class ServerOuterApi {
    private final DoranServer controller;
    private final NettyRemotingClient client;

    public ServerOuterApi(final DoranServer controller,
                          final NettyClientConfig config) {
        this.controller = controller;
        this.client = new NettyRemotingClient(config);
    }
}
