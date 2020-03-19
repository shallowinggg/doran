package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.client.Message;
import io.netty.util.concurrent.EventExecutor;
import org.jetbrains.annotations.Nullable;

/**
 * @author shallowinggg
 */
public interface BuiltInConsumer {
    Message receive();

    void setMessageListener(@Nullable MessageListener listener);

    @Nullable
    MessageListener getMessageListener();

    void register(EventExecutor executor);

    EventExecutor executor();
}
