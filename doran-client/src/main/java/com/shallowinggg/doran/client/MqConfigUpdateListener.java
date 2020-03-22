package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MQConfig;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * Interface to be implemented by MQ Config update event listeners.
 *
 * <p>Based on the standard {@code java.util.EventListener} interface
 * for the Observer design pattern.
 *
 * Client producer and consumer should implement this interface,
 * when config they used has updated, they should rebuild.
 *
 * @author shallowinggg
 */
@FunctionalInterface
public interface MqConfigUpdateListener extends EventListener {

    /**
     * Handle an MQ Config update event.
     *
     * @param newConfig config that has updated
     */
    void onMqConfigUpdate(@NotNull MQConfig newConfig);
}
