package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.client.common.Message;
import io.netty.util.concurrent.EventExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public interface BuiltInConsumer {
    Message receive();

    Message receive(long timeout, TimeUnit unit) throws InterruptedException;

    Set<MessageListener> getMessageListeners();

    void register(@NotNull EventExecutor executor);

    EventExecutor executor();
}
