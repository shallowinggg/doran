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
    private final RemotingClient client;
    private final ClientManageProcessor processor;
    private final ClientConfig clientConfig;
    private ScheduledExecutorService heartBeatExecutor;
    private ExecutorService requestConfigExecutor;

    public ClientApiImpl(final NettyClientConfig nettyClientConfig,
                         final ClientManageProcessor processor,
                         final ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.processor = processor;
        this.client = new NettyRemotingClient(nettyClientConfig);
    }

    public void init() {
        this.heartBeatExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryImpl("heartBeat_", true));
        this.requestConfigExecutor = new ThreadPoolExecutor(clientConfig.getRequestConfigThreadNum(),
                clientConfig.getRequestConfigMaxThreadNum(), 1, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(32),
                new ThreadFactoryImpl("requestMQConfig_"));
        this.registerProcessor();
    }

    public void start() {
        // 启动各种组件
        // 与server建立连接
        // 获取ip + pid
        this.heartBeatExecutor.scheduleAtFixedRate(this::sendHeartBeat,
                0, clientConfig.getHeartBeatServerInterval(), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {

    }

    public void registerProcessor() {
        this.client.registerProcessor(RequestCode.SEND_MQ_CONFIG, processor, null);
    }

    public void sendHeartBeat() {
        // 发送本机ip以及pid，以便失联以后server能够报错定位
    }

    public MqConfig requestConfig(String configName) {
        return null;
    }

}
