package com.shallowinggg.doran.common.util.concurrent;

import io.netty.util.concurrent.*;

import java.util.concurrent.Executor;
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
    protected EventExecutor newChild(Executor executor, Object... objects) throws Exception {
        return new DoranEventExecutor(this, executor, (Integer)objects[0], (RejectedExecutionHandler)objects[1]);
    }
}
