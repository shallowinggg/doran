package com.shallowinggg.doran.common.util.concurrent;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * @author shallowinggg
 */
public class DoranEventExecutor extends SingleThreadEventExecutor {
    static final int DEFAULT_MAX_PENDING_EXECUTOR_TASKS =
            Math.max(1024, SystemPropertyUtil.getInt("com.shallowinggg.eventExecutor.maxPendingTasks", 2147483647));
    private BlockingQueue<Runnable> workQueue;

    protected DoranEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory) {
        super(parent, threadFactory, true, DEFAULT_MAX_PENDING_EXECUTOR_TASKS, RejectedExecutionHandlers.reject());
    }

    protected DoranEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory,
                                 int maxPendingTasks, RejectedExecutionHandler rejectedHandler) {
        super(parent, threadFactory, true, maxPendingTasks, rejectedHandler);
    }

    public DoranEventExecutor(EventExecutorGroup parent, Executor executor, int maxPendingTasks,
                                RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, true, maxPendingTasks, rejectedExecutionHandler);
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        if (this.workQueue == null) {
            this.workQueue = new LinkedBlockingQueue<>(maxPendingTasks);
        }
        return this.workQueue;
    }

    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     */
    public List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>(q.size());
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    @Override
    protected void run() {
        do {
            this.runAllTasks();
        } while (!this.confirmShutdown());
    }
}
