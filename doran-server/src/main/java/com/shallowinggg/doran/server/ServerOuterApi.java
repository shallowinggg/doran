package com.shallowinggg.doran.server;

import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;

/**
 * @author shallowinggg
 */
public class ServerOuterApi {
    private final ServerController controller;
    private final NettyRemotingClient client;

    public ServerOuterApi(final ServerController controller,
                          final NettyClientConfig config) {
        this.controller = controller;
        this.client = new NettyRemotingClient(config);
    }
}
