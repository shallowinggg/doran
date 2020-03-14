package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.*;
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
    private final ServerController controller;

    public ServerCoreProcessor(final ServerController controller) {
        this.controller = controller;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        switch (request.getCode()) {
            case RequestCode.REGISTER_CLIENT:
                return registerClient(request);
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
    public RemotingCommand registerClient(final RemotingCommand request) throws RemotingCommandException {
        final RemotingCommand response = RemotingCommand.createResponseCommand(RegisterClientResponseHeader.class);
        final RegisterClientResponseHeader responseHeader = (RegisterClientResponseHeader) response.readCustomHeader();
        final RegisterClientRequestHeader requestHeader = request.decodeCommandCustomHeader(RegisterClientRequestHeader.class);

        String clientId = requestHeader.getClientId();
        String clientName = requestHeader.getClientName();
        final ClientManager manager = this.controller.getClientManager();

        if (!manager.hasClient(clientId)) {
            ClientMetaInfo clientMetaInfo = new ClientMetaInfo(clientId, clientName);
            manager.registerClient(clientMetaInfo);
            responseHeader.setHoldingMqConfigNums(0);
        } else {
            ClientMetaInfo clientMetaInfo = manager.getClientMetaInfo(clientId);
            final Collection<MqConfig> mqConfigs = clientMetaInfo.holdingConfigs();
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
        final RemotingCommand response = RemotingCommand.createResponseCommand(RequestMqConfigResponseHeader.class);
        final RequestMqConfigResponseHeader responseHeader = (RequestMqConfigResponseHeader) response.readCustomHeader();
        final RequestMqConfigRequestHeader requestHeader = request.decodeCommandCustomHeader(RequestMqConfigRequestHeader.class);

        String configName = requestHeader.getConfigName();
        final MqConfigManager mqConfigManager = this.controller.getMqConfigManager();

        if (mqConfigManager.containsConfig(configName)) {
            MqConfig config = mqConfigManager.getConfig(configName);
            responseHeader.setName(config.getName());
            responseHeader.setUrls(config.getUrls());
            responseHeader.setUsername(config.getUsername());
            responseHeader.setPassword(config.getPassword());
            responseHeader.setDomain(config.getDomain().getType());
            responseHeader.setDomainName(config.getDomainName());
            responseHeader.setThreadNum(config.getThreadNum());
            responseHeader.setTimestamp(config.getTimestamp());

            response.setCode(ResponseCode.SUCCESS);
        } else {
            response.setCode(ResponseCode.CONFIG_NOT_EXIST);
        }

        response.setRemark(null);
        return response;
    }
}
