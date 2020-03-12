package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.*;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import com.shallowinggg.doran.common.exception.NetworkException;
import com.shallowinggg.doran.common.exception.SystemException;
import com.shallowinggg.doran.common.exception.UnexpectedResponseException;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ClientApiImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApiImpl.class);
    private final ConfigController controller;
    private final RemotingClient client;
    private ExecutorService clientOuterExecutor;
    private String serverAddr;

    public ClientApiImpl(final ConfigController controller,
                         final NettyClientConfig nettyClientConfig) {
        this.controller = controller;
        this.client = new NettyRemotingClient(nettyClientConfig);
    }

    public void init() {
        this.registerProcessor();
        this.clientOuterExecutor = new ThreadPoolExecutor(2, 2,
                1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(32),
                new ThreadFactoryImpl("clientApi_"));
    }

    public void start() {
        this.client.start();
    }

    public void shutdown() {
        this.client.shutdown();
    }

    public void registerProcessor() {
        ClientManageProcessor processor = new ClientManageProcessor(this.controller);
        this.client.registerProcessor(RequestCode.UPDATE_MQ_CONFIG, processor, null);
    }

    public void updateServerAddress(String serverAddress) {
        this.serverAddr = serverAddress;
        this.client.updateNameServerAddressList(Collections.singletonList(serverAddress));
    }

    public void registerClient(final String clientId, final String clientName, int timeoutMillis) {
        final String serverAddr = this.serverAddr;
        final RegisterClientRequestHeader header = new RegisterClientRequestHeader();
        header.setClientId(clientId);
        header.setClientName(clientName);

        clientOuterExecutor.execute(() -> {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REGISTER_CLIENT, header);
            try {
                RemotingCommand response = this.client.invokeSync(serverAddr, request, timeoutMillis);
                switch (response.getCode()) {
                    case ResponseCode.SUCCESS:
                        LOGGER.info("Register client {} to server {} success", clientId, serverAddr);
                        break;
                    default:
                        throw new UnexpectedResponseException(response.getCode(), "REGISTER_CLIENT");
                }
            } catch (InterruptedException | RemotingConnectException |
                    RemotingSendRequestException | RemotingTimeoutException e) {
                LOGGER.error("Register client {} to server {} fail", clientId, serverAddr, e);
            }
        });
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


}
