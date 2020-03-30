package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.common.util.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author shallowinggg
 */
public abstract class AbstractBuiltInConsumer implements BuiltInConsumer {
    @Nullable
    private final ThreadPoolExecutor executor;

    @Nullable
    private final Set<MessageListener> listeners;

    protected AbstractBuiltInConsumer(@Nullable ThreadPoolExecutor executor,
                                      @Nullable Set<MessageListener> listeners) {
        this.executor = executor;
        this.listeners = listeners;
    }

    @Override
    public ThreadPoolExecutor executor() {
        return executor;
    }

    @Override
    public Set<MessageListener> getMessageListeners() {
        if(CollectionUtils.isNotEmpty(listeners)) {
            return Collections.unmodifiableSet(this.listeners);
        }
        return Collections.emptySet();
    }
}
