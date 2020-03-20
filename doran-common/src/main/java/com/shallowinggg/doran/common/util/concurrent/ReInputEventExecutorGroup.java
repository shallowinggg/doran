package com.shallowinggg.doran.common.util.concurrent;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;

import java.util.concurrent.ThreadFactory;

/**
 * @author shallowinggg
 */
public class ReInputEventExecutorGroup extends MultithreadEventExecutorGroup {
    public ReInputEventExecutorGroup(int nThreads) {
        this(nThreads, null);
    }

    public ReInputEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, DoranEventExecutor.DEFAULT_MAX_PENDING_EXECUTOR_TASKS, RejectedExecutionHandlers.reject());
    }

    public ReInputEventExecutorGroup(int nThreads, ThreadFactory threadFactory, int maxPendingTasks, RejectedExecutionHandler rejectedHandler) {
        super(nThreads, threadFactory, maxPendingTasks, rejectedHandler);
    }

    @Override
    protected EventExecutor newChild(ThreadFactory threadFactory, Object... args) throws Exception {
        return new ReInputEventExecutor(this, threadFactory, (Integer)args[0], (RejectedExecutionHandler)args[1]);
    }

    @Override
    public ReInputEventExecutor next() {
        return (ReInputEventExecutor) super.next();
    }
}
