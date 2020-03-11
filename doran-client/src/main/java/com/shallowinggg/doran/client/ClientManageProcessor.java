package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.RequestCode;
import com.shallowinggg.doran.transport.netty.NettyRequestProcessor;
import com.shallowinggg.doran.transport.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author shallowinggg
 */
public class ClientManageProcessor implements NettyRequestProcessor {
    private final ConfigController controller;

    public ClientManageProcessor(ConfigController controller) {
        this.controller = controller;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        switch (request.getCode()) {
            case RequestCode.SEND_MQ_CONFIG:
            case RequestCode.HEART_BEAT:
            default:
        }

        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
