package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.common.util.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author shallowinggg
 */
public abstract class AbstractBuiltInConsumer implements BuiltInConsumer {
    @Nullable
    private final ThreadPoolExecutor executor;

    @Nullable
    private final List<MessageListener> listeners;

    protected AbstractBuiltInConsumer(@Nullable ThreadPoolExecutor executor,
                                      @Nullable List<MessageListener> listeners) {
        this.executor = executor;
        this.listeners = listeners;
    }

    @Override
    public ThreadPoolExecutor executor() {
        return executor;
    }

    @Override
    public List<MessageListener> getMessageListeners() {
        if(CollectionUtils.isNotEmpty(listeners)) {
            return Collections.unmodifiableList(this.listeners);
        }
        return Collections.emptyList();
    }
}
