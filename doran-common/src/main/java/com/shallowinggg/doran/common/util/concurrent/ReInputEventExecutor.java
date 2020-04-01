package com.shallowinggg.doran.common.util.concurrent;

import com.shallowinggg.doran.common.util.Assert;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author shallowinggg
 */
public class ReInputEventExecutor extends SingleThreadEventExecutor {
    static final int DEFAULT_MAX_REBUILDING_TEMP_TASKS =
            Math.max(1024, SystemPropertyUtil.getInt("com.shallowinggg.producer.maxRebuildingTempTasks", 2147483647));
    private BlockingQueue<Runnable> taskQueue;
    private EventExecutorGroup transferGroup;
    private final Semaphore lock = new Semaphore(0);

    protected ReInputEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory) {
        super(parent, threadFactory, true, DEFAULT_MAX_REBUILDING_TEMP_TASKS, RejectedExecutionHandlers.reject());
    }

    protected ReInputEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory,
                                   int maxPendingTasks, RejectedExecutionHandler rejectedHandler) {
        super(parent, threadFactory, true, maxPendingTasks, rejectedHandler);
    }

    public ReInputEventExecutor(EventExecutorGroup parent, Executor executor, int maxPendingTasks,
                                RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, true, maxPendingTasks, rejectedExecutionHandler);
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        if (this.taskQueue == null) {
            this.taskQueue = new LinkedBlockingQueue<>(maxPendingTasks);
        }
        return this.taskQueue;
    }

    public void setTransferGroup(@NotNull final EventExecutorGroup transferGroup) {
        Assert.notNull(transferGroup, "'transferGroup' must not be null");
        this.transferGroup = transferGroup;
        lock.release();
    }

    @Override
    protected void run() {
        do {
            this.lock.acquireUninterruptibly();
            do {
                Runnable task = this.pollTask();
                if (task != null) {
                    transferGroup.execute(task);
                    this.updateLastExecutionTime();
                }
            } while (this.hasTasks());
            this.transferGroup = null;
        } while (!this.confirmShutdown());
    }
}
