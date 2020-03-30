package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.client.common.Message;

import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public interface BuiltInConsumer {
    Message receive();

    Message receive(long timeout, TimeUnit unit) throws InterruptedException;

    Set<MessageListener> getMessageListeners();

    ThreadPoolExecutor executor();
}
