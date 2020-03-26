package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import io.netty.util.concurrent.EventExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * @author shallowinggg
 */
public abstract class AbstractBuiltInConsumer implements BuiltInConsumer {
    // TODO: 一个consumer持有一个线程池，多个线程
    private EventExecutor executor;
    private final Set<MessageListener> listeners;

    protected AbstractBuiltInConsumer() {
        this.listeners = Collections.emptySet();
    }

    protected AbstractBuiltInConsumer(Set<MessageListener> listeners) {
        Assert.isTrue(CollectionUtils.isNotEmpty(listeners), "'listeners' must not be empty");
        this.listeners = listeners;
    }

    @Override
    public void register(@NotNull EventExecutor executor) {
        Assert.notNull(executor, "'executor' must not be null");
        this.executor = executor;
    }

    @Override
    public EventExecutor executor() {
        return executor;
    }

    @Override
    public Set<MessageListener> getMessageListeners() {
        return Collections.unmodifiableSet(this.listeners);
    }
}
