package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.client.Message;

import java.util.EventListener;

/**
 * @author shallowinggg
 */
public interface MessageListener extends EventListener {
    boolean onMessage(Message message);
}
