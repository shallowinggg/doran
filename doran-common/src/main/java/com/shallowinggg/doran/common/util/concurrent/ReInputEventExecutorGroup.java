package com.shallowinggg.doran.common.util.concurrent;

import com.shallowinggg.doran.common.util.Assert;
import io.netty.util.concurrent.*;
import org.jetbrains.annotations.NotNull;

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
        return new ReInputEventExecutor(this, threadFactory, (Integer) args[0], (RejectedExecutionHandler) args[1]);
    }

    @Override
    public EventExecutor next() {
        return super.next();
    }

    /**
     * Set transfer EventExecutorGroup for every child.
     * <p>
     * This will notify child to transfer its all tasks
     * to the given group.
     *
     * @param transferGroup the EventExecutorGroup that tasks will be transferred to
     */
    public void setTransferGroup(@NotNull final EventExecutorGroup transferGroup) {
        Assert.notNull(transferGroup, "'transferGroup' must not be null");
        for (EventExecutor e : children()) {
            ((ReInputEventExecutor) e).setTransferGroup(transferGroup);
        }
    }
}
