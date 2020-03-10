package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.common.RequestCode;
import com.shallowinggg.doran.common.ThreadFactoryImpl;
import com.shallowinggg.doran.transport.RemotingClient;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyRemotingClient;

import java.util.concurrent.*;

/**
 * @author shallowinggg
 */
public class ClientApiImpl {
    private final ConfigController controller;
    private final RemotingClient client;
    private final ClientManageProcessor processor;
    private final ClientConfig clientConfig;
    private ScheduledExecutorService heartBeatExecutor;

    public ClientApiImpl(final ConfigController controller,
                         final NettyClientConfig nettyClientConfig,
                         final ClientManageProcessor processor,
                         final ClientConfig clientConfig) {
        this.controller = controller;
        this.clientConfig = clientConfig;
        this.processor = processor;
        this.client = new NettyRemotingClient(nettyClientConfig);
    }

    public void init() {
        this.heartBeatExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryImpl("heartBeat_", true));
        this.heartBeatExecutor.schedule(this::sendHeartBeat,
                clientConfig.getHeartBeatServerInterval(), TimeUnit.MILLISECONDS);
        this.registerProcessor();
    }

    public void registerProcessor() {
        this.client.registerProcessor(RequestCode.SEND_MQ_CONFIG, processor, null);
    }

    public void sendHeartBeat() {

    }

    public MqConfig requestConfig(String configName) {
        return null;
    }

}
