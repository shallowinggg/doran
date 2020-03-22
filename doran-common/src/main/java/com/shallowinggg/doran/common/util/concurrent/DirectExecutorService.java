package com.shallowinggg.doran.common.util.concurrent;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 * @since 3.0
 */
public class DirectExecutorService extends AbstractExecutorService {

    /**
     * Lock used whenever accessing the state variables (runningTasks, shutdown) of the executor
     */
    private final Object lock = new Object();

    /**
     * Conceptually, these two variables describe the executor being in
     * one of three states:
     * - Active: shutdown == false
     * - Shutdown: runningTasks > 0 and shutdown == true
     * - Terminated: runningTasks == 0 and shutdown == true
     */
    @GuardedBy("lock")
    private int runningTasks = 0;

    @GuardedBy("lock")
    private boolean shutdown = false;

    @Override
    public void execute(Runnable command) {
        startTask();
        try {
            command.run();
        } finally {
            endTask();
        }
    }

    @Override
    public boolean isShutdown() {
        synchronized (lock) {
            return shutdown;
        }
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            if (runningTasks == 0) {
                lock.notifyAll();
            }
        }
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminated() {
        synchronized (lock) {
            return shutdown && runningTasks == 0;
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        synchronized (lock) {
            while (true) {
                if (shutdown && runningTasks == 0) {
                    return true;
                } else if (nanos <= 0) {
                    return false;
                } else {
                    long now = System.nanoTime();
                    TimeUnit.NANOSECONDS.timedWait(lock, nanos);
                    nanos -= System.nanoTime() - now;
                }
            }
        }
    }

    /**
     * Checks if the executor has been shut down and increments the running task count.
     *
     * @throws RejectedExecutionException if the executor has been previously shutdown
     */
    private void startTask() {
        synchronized (lock) {
            if (shutdown) {
                throw new RejectedExecutionException("Executor already shutdown");
            }
            runningTasks++;
        }
    }

    /**
     * Decrements the running task count.
     */
    private void endTask() {
        synchronized (lock) {
            int numRunning = --runningTasks;
            if (numRunning == 0) {
                lock.notifyAll();
            }
        }
    }
}
