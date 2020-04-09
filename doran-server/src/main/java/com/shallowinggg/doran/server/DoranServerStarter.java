package com.shallowinggg.doran.server;

import com.shallowinggg.doran.server.transport.ServerConfig;
import com.shallowinggg.doran.transport.netty.NettyClientConfig;
import com.shallowinggg.doran.transport.netty.NettyServerConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

/**
 * @author shallowinggg
 */
@SpringBootApplication
@EnableCaching
public class DoranServerStarter {

    @Bean
    public ServerConfig serverConfig() {
        return new ServerConfig();
    }

    @Bean
    public NettyServerConfig nettyServerConfig() {
        return new NettyServerConfig();
    }

    @Bean
    public NettyClientConfig nettyClientConfig() {
        return new NettyClientConfig();
    }
}
