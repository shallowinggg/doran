package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.common.util.Assert;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author shallowinggg
 */
public abstract class AbstractBuiltInProducer implements BuiltInProducer {
    private EventExecutor executor;

    @Override
    public void register(EventExecutor executor) {
        Assert.notNull(executor, "'executor' must not be null");
        this.executor = executor;
    }

    @Override
    public EventExecutor executor() {
        return executor;
    }
}
