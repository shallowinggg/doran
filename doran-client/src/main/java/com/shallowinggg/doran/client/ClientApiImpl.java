package com.shallowinggg.doran.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shallowinggg.doran.client.resolver.DefaultInetAddressChecker;
import com.shallowinggg.doran.client.resolver.InetAddressChecker;
import com.shallowinggg.doran.common.*;
import com.shallowinggg.doran.common.exception.ConfigNotExistException;
import com.shallowinggg.doran.common.exception.UnexpectedResponseException;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.PojoHeaderConverter;
import com.shallowinggg.doran.common.util.retry.*;
import com.shallowinggg.doran.transport.RemotingClient;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;
import com.shallowinggg.doran.transport.protocol.RemotingCommand;
import com.shallowinggg.doran.transport.protocol.RemotingSerializable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

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
    private static final Supplier<Retryer<Object>> RETRY_TASK = () -> RetryerBuilder.newBuilder()
            .retryIfException()
            .withWaitStrategy(WaitStrategies.fibonacciWait(10L, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(5))
            .build();

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
     * If appear network problems like timeout, data corruption etc.
     * this method will retry at most 5 times.
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

        try {
            RETRY_TASK.get().call(() -> {
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
                            return null;
                        default:
                            throw new UnexpectedResponseException(response.getCode(), "REGISTER_CLIENT");
                    }
                } catch (InterruptedException | RemotingCommandException e) {
                    // this exceptions will be handle in RetryException catch block
                    // if retry fail.
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Register client {} to server {} fail", clientId, serverAddr, e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // handle RuntimeException for UnexpectedResponseException
            RuntimeException cause = (RuntimeException) e.getCause();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Register client {} to server {} fail", clientId, serverAddr, cause);
            }
            throw cause;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Register client {} to server {} fail, cause: retry count {} has exhausted",
                        clientId, serverAddr, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    public void sendHeartBeat() {
        // 发送本机ip以及pid，以便失联以后server能够报错定位

    }


    /**
     * Request MQ config with server {@link #serverAddr}. If appear network
     * problems like timeout, data corruption etc., this method will
     * retry at most 5 times.
     *
     * @param configName    the name of request config
     * @param timeoutMillis timeout for per network communication
     * @return MQ Config that request
     * @throws ConfigNotExistException      if request config is not exist
     * @throws UnexpectedResponseException  if the response of server incorrectly
     * @throws RetryCountExhaustedException if retry count has exhausted
     */
    public MQConfig requestConfig(String configName, int timeoutMillis)
            throws ConfigNotExistException, UnexpectedResponseException, RetryCountExhaustedException {
        final RequestMQConfigRequestHeader header = new RequestMQConfigRequestHeader();
        header.setConfigName(configName);

        try {
            return (MQConfig) RETRY_TASK.get().call(() -> {
                RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REQUEST_CONFIG, header);
                try {
                    RemotingCommand response = this.client.invokeSync(null, request, timeoutMillis);

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
                } catch (InterruptedException | RemotingCommandException | JsonProcessingException e) {
                    // this exceptions will be handle in RetryException catch block
                    // if retry fail.
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Request config {} fail", configName, e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // handle RuntimeException for ConfigNotExistException and UnexpectedResponseException
            RuntimeException cause = (RuntimeException) e.getCause();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request config {} fail", configName, cause);
            }
            throw cause;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request config {} fail, cause: retry count {} has exhausted",
                        configName, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

}
