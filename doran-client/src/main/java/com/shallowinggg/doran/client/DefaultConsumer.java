package com.shallowinggg.doran.client;

import com.shallowinggg.doran.client.common.MqConfigBean;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;

/**
 * @author shallowinggg
 */
public class DefaultConsumer implements MqConfigBean {
    private MQConfig config;

    @Override
    public void setMqConfig(@NotNull MQConfig newConfig) {
        Assert.notNull(newConfig, "'newConfig' must not be null");
    }

    @Override
    public MQConfig getMqConfig() {
        return config;
    }
}