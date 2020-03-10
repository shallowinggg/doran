package com.shallowinggg.doran.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shallowinggg
 */
public class ThreadFactoryImpl implements ThreadFactory {
    private final String prefix;
    private final boolean daemon;
    private final AtomicInteger index = new AtomicInteger(1);

    public ThreadFactoryImpl(String prefix) {
        this(prefix, false);
    }

    public ThreadFactoryImpl(String prefix, boolean daemon) {
        this.prefix = prefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + index.getAndIncrement());
        t.setDaemon(daemon);
        return t;
    }
}
