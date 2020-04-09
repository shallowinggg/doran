package com.shallowinggg.doran.server.transport;

import com.shallowinggg.doran.common.*;
import com.shallowinggg.doran.common.util.PojoHeaderConverter;
import com.shallowinggg.doran.common.RequestMQConfigResponseHeader;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;
import com.shallowinggg.doran.transport.netty.NettyRequestProcessor;
import com.shallowinggg.doran.transport.protocol.RemotingCommand;
import com.shallowinggg.doran.transport.protocol.RemotingSerializable;
import io.netty.channel.ChannelHandlerContext;

import java.util.Collection;

/**
 * @author shallowinggg
 */
public class ServerCoreProcessor implements NettyRequestProcessor {
    private final DoranServer controller;

    public ServerCoreProcessor(final DoranServer controller) {
        this.controller = controller;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        switch (request.getCode()) {
            case RequestCode.REGISTER_CLIENT:
                return registerClient(ctx, request);
            case RequestCode.HEART_BEAT:
                break;
            case RequestCode.REQUEST_CONFIG:
                return requestMqConfig(request);
            default:
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    /**
     * Handle client's request {@link RequestCode#REGISTER_CLIENT}.
     * If the client is registered for the first time, it will be
     * recorded and response normally.
     * If the client is downtime and restart quickly (server has
     * not removed inactive client yet), this method will return
     * MQ Configs it requests before, and this can help it recover.
     *
     * @param request request to handle
     * @return handle result
     * @throws RemotingCommandException if read request's header fail
     */
    public RemotingCommand registerClient(final ChannelHandlerContext context,
                                          final RemotingCommand request)
            throws RemotingCommandException {
        final RemotingCommand response = RemotingCommand.createResponseCommand(RegisterClientResponseHeader.class);
        final RegisterClientResponseHeader responseHeader = (RegisterClientResponseHeader) response.readCustomHeader();
        final RegisterClientRequestHeader requestHeader = request.decodeCommandCustomHeader(RegisterClientRequestHeader.class);

        String clientId = requestHeader.getClientId();
        String clientName = requestHeader.getClientName();
        final ClientManager manager = this.controller.getClientManager();

        if (!manager.hasClient(clientId)) {
            ClientMetaInfo clientMetaInfo = new ClientMetaInfo(clientId, clientName);
            manager.registerClient(clientMetaInfo, context.channel());
            responseHeader.setHoldingMqConfigNums(0);
        } else {
            ClientMetaInfo clientMetaInfo = manager.getClientMetaInfo(clientId);
            final Collection<MQConfig> mqConfigs = clientMetaInfo.holdingConfigs();
            if (mqConfigs.isEmpty()) {
                responseHeader.setHoldingMqConfigNums(0);
            } else {
                responseHeader.setHoldingMqConfigNums(mqConfigs.size());
                byte[] json = RemotingSerializable.encode(mqConfigs);
                response.setBody(json);
            }
        }

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    /**
     * Handle client's request {@link RequestCode#REQUEST_CONFIG}.
     * If the config requested exist, it will be returned normally.
     * Otherwise, this method will return {@link ResponseCode#CONFIG_NOT_EXIST}.
     *
     * @param request request to handle
     * @return handle result
     * @throws RemotingCommandException if read request's header fail
     */
    public RemotingCommand requestMqConfig(final RemotingCommand request) throws RemotingCommandException {
        final RemotingCommand response = RemotingCommand.createResponseCommand(RequestMQConfigResponseHeader.class);
        final RequestMQConfigResponseHeader responseHeader = (RequestMQConfigResponseHeader) response.readCustomHeader();
        final RequestMQConfigRequestHeader requestHeader = request.decodeCommandCustomHeader(RequestMQConfigRequestHeader.class);

        String configName = requestHeader.getConfigName();
        final MQConfigManager mqConfigManager = this.controller.getMqConfigManager();

        if (mqConfigManager.containsConfig(configName)) {
            MQConfig config = mqConfigManager.getConfig(configName);
            PojoHeaderConverter.mqConfig2ResponseHeader(config, responseHeader);
            response.setCode(ResponseCode.SUCCESS);
        } else {
            response.setCode(ResponseCode.CONFIG_NOT_EXIST);
        }

        response.setRemark(null);
        return response;
    }
}
