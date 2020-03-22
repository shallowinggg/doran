package com.shallowinggg.doran.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shallowinggg.doran.client.resolver.DefaultInetAddressChecker;
import com.shallowinggg.doran.client.resolver.InetAddressChecker;
import com.shallowinggg.doran.common.*;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import com.shallowinggg.doran.common.exception.NetworkException;
import com.shallowinggg.doran.common.exception.SystemException;
import com.shallowinggg.doran.common.exception.UnexpectedResponseException;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.PojoHeaderConverter;
import com.shallowinggg.doran.common.util.retry.Retryer;
import com.shallowinggg.doran.common.util.retry.RetryerBuilder;
import com.shallowinggg.doran.common.util.retry.StopStrategies;
import com.shallowinggg.doran.common.util.retry.WaitStrategies;
import com.shallowinggg.doran.transport.RemotingClient;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;
import com.shallowinggg.doran.transport.exception.RemotingConnectException;
import com.shallowinggg.doran.transport.exception.RemotingSendRequestException;
import com.shallowinggg.doran.transport.exception.RemotingTimeoutException;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;
import com.shallowinggg.doran.transport.protocol.RemotingCommand;
import com.shallowinggg.doran.transport.protocol.RemotingSerializable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author shallowinggg
 */
public class ClientApiImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApiImpl.class);
    private final ClientController controller;
    private final RemotingClient client;
    private final InetAddressChecker nameResolver;
    private ExecutorService clientOuterExecutor;
    private String serverAddr;

    public ClientApiImpl(final ClientController controller,
                         final NettyClientConfig nettyClientConfig) {
        this.controller = controller;
        this.client = new NettyRemotingClient(nettyClientConfig);
        this.nameResolver = new DefaultInetAddressChecker();
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

    public void updateServerAddress(@NotNull String serverAddress) {
        Assert.hasText(serverAddress);
        this.nameResolver.check(serverAddress);
        this.serverAddr = serverAddress;
        this.client.updateNameServerAddressList(Collections.singletonList(serverAddress));
    }

    /**
     * Register current client to server with its id and name.
     * The main purpose of this method is creating connection
     * with server in advance.
     * <p>
     * This method will be invoked async, if there are any errors occur
     * in this process, it only prints log. The process that send
     * heart beat later will does this again.
     * <p>
     * If the client is fail in accident and recovers immediately,
     * this method can retrieve all mq configs request before.
     *
     * @param clientId      client's id to be registered
     * @param clientName    client' name to be registered
     * @param timeoutMillis transport timeout millis
     */
    public void registerClient(final String clientId, final String clientName, int timeoutMillis) {
        final String serverAddr = this.serverAddr;
        final RegisterClientRequestHeader header = new RegisterClientRequestHeader();
        header.setClientId(clientId);
        header.setClientName(clientName);

        Retryer<Object> retry = RetryerBuilder.newBuilder().retryIfException()
                .withWaitStrategy(WaitStrategies.fibonacciWait(10L, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .withRetryListener(attempt -> {
                    if(LOGGER.isInfoEnabled()) {
                        LOGGER.info("");
                    }
                }).build();

        clientOuterExecutor.execute(() -> {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REGISTER_CLIENT, header);
            try {
                RemotingCommand response = this.client.invokeSync(null, request, timeoutMillis);
                switch (response.getCode()) {
                    case ResponseCode.SUCCESS:
                        RegisterClientResponseHeader responseHeader = response.decodeCommandCustomHeader(RegisterClientResponseHeader.class);
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Register client {} to server {} success", clientId, serverAddr);
                        }

                        int configNums = responseHeader.getHoldingMqConfigNums();
                        if (configNums != 0) {
                            List<MQConfig> configs = RemotingSerializable.decodeArray(response.getBody(), MQConfig.class);
                            this.controller.getConfigManager().registerMqConfigs(configs);
                        }
                        break;
                    default:
                        throw new UnexpectedResponseException(response.getCode(), "REGISTER_CLIENT");
                }
            } catch (InterruptedException | RemotingConnectException |
                    RemotingSendRequestException | RemotingTimeoutException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Register client {} to server {} fail", clientId, serverAddr, e);
                }
            } catch (RemotingCommandException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Decode RegisterClientResponseHeader fail", e);
                }
            }
        });
    }

    public void sendHeartBeat() {
        // 发送本机ip以及pid，以便失联以后server能够报错定位

    }


    public MQConfig requestConfig(String configName, int timeoutMillis) {
        final RequestMQConfigRequestHeader header = new RequestMQConfigRequestHeader();
        header.setConfigName(configName);

        // TODO: 增加重试逻辑
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REQUEST_CONFIG, header);
        RemotingCommand response = null;
        try {
            response = this.client.invokeSync(null, request, timeoutMillis);

            switch (response.getCode()) {
                case ResponseCode.SUCCESS:
                    RequestMQConfigResponseHeader responseHeader = response.decodeCommandCustomHeader(RequestMQConfigResponseHeader.class);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Request MQ Config {} success", configName);
                    }

                    return PojoHeaderConverter.responseHeader2MQConfig(responseHeader);
                case ResponseCode.CONFIG_NOT_EXIST:
                    throw new ConfigNotExistException(configName);
                default:
                    throw new UnexpectedResponseException(response.getCode(), "REQUEST_CONFIG");
            }
        } catch (InterruptedException | RemotingConnectException |
                RemotingSendRequestException | RemotingTimeoutException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request config {} fail", configName, e);
            }
            throw new NetworkException();
        } catch (RemotingCommandException | JsonProcessingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request config {} fail, response: {}", configName, response, e);
            }
            throw new SystemException(e);
        }
    }

    @Immutable
    private static class RetryExceptionPredictor implements Predicate<Throwable> {
        private static final RetryExceptionPredictor INSTANCE = new RetryExceptionPredictor();

        RetryExceptionPredictor create() {
            return INSTANCE;
        }

        @Override
        public boolean test(Throwable throwable) {
            return throwable instanceof RemotingCommandException ||
                    throwable instanceof InterruptedException;
        }
    }

}
