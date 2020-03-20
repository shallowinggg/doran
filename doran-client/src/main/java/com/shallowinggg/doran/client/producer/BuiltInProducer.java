package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.Message;
import io.netty.util.concurrent.EventExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public interface BuiltInProducer {
    String sendMessage(Message message);

    String sendMessage(Message message, int delay, TimeUnit unit);

    void register(EventExecutor executor);

    @Nullable
    EventExecutor executor();
}
