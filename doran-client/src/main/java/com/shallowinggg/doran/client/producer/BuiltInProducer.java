package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.Message;
import io.netty.util.concurrent.EventExecutor;

import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public interface BuiltInProducer {
    void sendMessage(Message message);

    void sendMessage(Message message, int delay, TimeUnit unit);

    void register(EventExecutor executor);

    EventExecutor executor();

    void close();
}
