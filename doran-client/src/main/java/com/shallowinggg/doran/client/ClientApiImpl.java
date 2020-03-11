package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.*;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import com.shallowinggg.doran.common.exception.NetworkException;
import com.shallowinggg.doran.common.exception.SystemException;
import com.shallowinggg.doran.common.exception.UnexpectedResponseException;
import com.shallowinggg.doran.common.util.SystemUtils;
import com.shallowinggg.doran.transport.RemotingClient;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;
import com.shallowinggg.doran.transport.exception.RemotingConnectException;
import com.shallowinggg.doran.transport.exception.RemotingSendRequestException;
import com.shallowinggg.doran.transport.exception.RemotingTimeoutException;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;
import com.shallowinggg.doran.transport.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * @author shallowinggg
 */
public class ClientApiImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApiImpl.class);
    private final ConfigController controller;
    private final RemotingClient client;
    private final String clientId;
    private String serverAddr;

    public ClientApiImpl(final ConfigController controller,
                         final NettyClientConfig nettyClientConfig) {
        this.controller = controller;
        this.client = new NettyRemotingClient(nettyClientConfig);
        this.clientId = createClientId();
    }

    public void init() {
        this.registerProcessor();
    }

    public void start() {
        this.client.start();
    }

    public void shutdown() {
        this.client.shutdown();
    }

    public void registerProcessor() {
        ClientManageProcessor processor = new ClientManageProcessor(this.controller);
        this.client.registerProcessor(RequestCode.SEND_MQ_CONFIG, processor, null);
    }

    public void updateServerAddress(String serverAddress) {
        this.serverAddr = serverAddress;
        this.client.updateNameServerAddressList(Collections.singletonList(serverAddress));
    }

    public void sendHeartBeat() {
        // 发送本机ip以及pid，以便失联以后server能够报错定位

    }

    public MqConfig requestConfig(String configName, int timeoutMillis)
            throws NetworkException {
        final RequestMqConfigRequestHeader header = new RequestMqConfigRequestHeader();
        header.setConfigName(configName);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REQUEST_CONFIG, header);
        try {
            RemotingCommand response = this.client.invokeSync(serverAddr, request, timeoutMillis);

            switch (response.getCode()) {
                case ResponseCode.SUCCESS:
                    SendMqConfigResponseHeader responseHeader = response.decodeCommandCustomHeader(SendMqConfigResponseHeader.class);
                    return MqConfig.obtainFromMqConfigHeader(responseHeader);
                case ResponseCode.CONFIG_NOT_EXIST:
                    throw new ConfigNotExistException(configName);
                default:
                    throw new UnexpectedResponseException(response.getCode(), "REQUEST_CONFIG");
            }
        } catch (InterruptedException | RemotingConnectException |
                RemotingSendRequestException | RemotingTimeoutException e) {
            LOGGER.error("request config {} fail", configName, e);
            throw new NetworkException();
        } catch (RemotingCommandException e) {
            LOGGER.error("request config {} fail", configName, e);
            throw new SystemException(e);
        }
    }

    /**
     * Create the unique client id, format "ip:pid".
     *
     * @return client id
     */
    private String createClientId() {
        byte[] ip = SystemUtils.getIp();
        if (ip == null) {
            ip = SystemUtils.createFakeIP();
        }

        int pid = SystemUtils.getPid();
        StringBuilder clientId = new StringBuilder(15 + 1 + 5);
        for (int i = 0, len = ip.length; i < len; ++i) {
            clientId.append(ip[i]);
            if (i != len - 1) {
                clientId.append(".");
            }
        }
        clientId.append(':').append(pid);
        return clientId.toString();
    }

}
