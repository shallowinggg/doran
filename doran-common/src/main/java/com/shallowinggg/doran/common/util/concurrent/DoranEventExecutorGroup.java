package com.shallowinggg.doran.common.util.concurrent;

import io.netty.util.concurrent.*;

import java.util.concurrent.ThreadFactory;

/**
 * @author shallowinggg
 */
public class DoranEventExecutorGroup extends MultithreadEventExecutorGroup {
    public DoranEventExecutorGroup(int nThreads) {
        this(nThreads, null);
    }

    public DoranEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, DoranEventExecutor.DEFAULT_MAX_PENDING_EXECUTOR_TASKS, RejectedExecutionHandlers.reject());
    }

    public DoranEventExecutorGroup(int nThreads, ThreadFactory threadFactory, int maxPendingTasks, RejectedExecutionHandler rejectedHandler) {
        super(nThreads, threadFactory, maxPendingTasks, rejectedHandler);
    }

    @Override
    protected EventExecutor newChild(ThreadFactory threadFactory, Object... args) throws Exception {
        return new DoranEventExecutor(this, threadFactory, (Integer)args[0], (RejectedExecutionHandler)args[1]);
    }
}
