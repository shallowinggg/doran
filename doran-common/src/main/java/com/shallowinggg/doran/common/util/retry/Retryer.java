package com.shallowinggg.doran.common.util.retry;

import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * A retryer, which executes a call, and retries it until it succeeds, or
 * a stop strategy decides to stop retrying. A wait strategy is used to sleep
 * between attempts. The strategy to decide if the call succeeds or not is
 * also configurable.
 * <p></p>
 * A retryer can also wrap the callable into a RetryerCallable, which can be submitted to an executor.
 * <p></p>
 * Retryer instances are better constructed with a {@link RetryerBuilder}. A retryer
 * is thread-safe, provided the arguments passed to its constructor are thread-safe.
 *
 * @param <V> the type of the call return value
 * @author JB
 * @author Jason Dunkelberger (dirkraft)
 */
public final class Retryer<V> {
    private final StopStrategy stopStrategy;
    private final WaitStrategy waitStrategy;
    private final BlockStrategy blockStrategy;
    private final AttemptTimeLimiter<V> attemptTimeLimiter;
    private final Predicate<Attempt<V>> rejectionPredicate;
    private final Collection<RetryListener<V>> listeners;

    /**
     * Constructor
     *
     * @param stopStrategy       the strategy used to decide when the retryer must stop retrying
     * @param waitStrategy       the strategy used to decide how much time to sleep between attempts
     * @param rejectionPredicate the predicate used to decide if the attempt must be rejected
     *                           or not. If an attempt is rejected, the retryer will retry the call, unless the stop
     *                           strategy indicates otherwise or the thread is interrupted.
     */
    public Retryer(@NotNull StopStrategy stopStrategy,
                   @NotNull WaitStrategy waitStrategy,
                   @NotNull Predicate<Attempt<V>> rejectionPredicate) {

        this(AttemptTimeLimiters.noTimeLimit(), stopStrategy, waitStrategy, BlockStrategies.threadSleepStrategy(), rejectionPredicate);
    }

    /**
     * Constructor
     *
     * @param attemptTimeLimiter to prevent from any single attempt from spinning infinitely
     * @param stopStrategy       the strategy used to decide when the retryer must stop retrying
     * @param waitStrategy       the strategy used to decide how much time to sleep between attempts
     * @param rejectionPredicate the predicate used to decide if the attempt must be rejected
     *                           or not. If an attempt is rejected, the retryer will retry the call, unless the stop
     *                           strategy indicates otherwise or the thread is interrupted.
     */
    public Retryer(@NotNull AttemptTimeLimiter<V> attemptTimeLimiter,
                   @NotNull StopStrategy stopStrategy,
                   @NotNull WaitStrategy waitStrategy,
                   @NotNull Predicate<Attempt<V>> rejectionPredicate) {
        this(attemptTimeLimiter, stopStrategy, waitStrategy, BlockStrategies.threadSleepStrategy(), rejectionPredicate);
    }

    /**
     * Constructor
     *
     * @param attemptTimeLimiter to prevent from any single attempt from spinning infinitely
     * @param stopStrategy       the strategy used to decide when the retryer must stop retrying
     * @param waitStrategy       the strategy used to decide how much time to sleep between attempts
     * @param blockStrategy      the strategy used to decide how to block between retry attempts; eg, Thread#sleep(), latches, etc.
     * @param rejectionPredicate the predicate used to decide if the attempt must be rejected
     *                           or not. If an attempt is rejected, the retryer will retry the call, unless the stop
     *                           strategy indicates otherwise or the thread is interrupted.
     */
    public Retryer(@NotNull AttemptTimeLimiter<V> attemptTimeLimiter,
                   @NotNull StopStrategy stopStrategy,
                   @NotNull WaitStrategy waitStrategy,
                   @NotNull BlockStrategy blockStrategy,
                   @NotNull Predicate<Attempt<V>> rejectionPredicate) {
        this(attemptTimeLimiter, stopStrategy, waitStrategy, blockStrategy, rejectionPredicate, new ArrayList<>());
    }

    /**
     * Constructor
     *
     * @param attemptTimeLimiter to prevent from any single attempt from spinning infinitely
     * @param stopStrategy       the strategy used to decide when the retryer must stop retrying
     * @param waitStrategy       the strategy used to decide how much time to sleep between attempts
     * @param blockStrategy      the strategy used to decide how to block between retry attempts; eg, Thread#sleep(), latches, etc.
     * @param rejectionPredicate the predicate used to decide if the attempt must be rejected
     *                           or not. If an attempt is rejected, the retryer will retry the call, unless the stop
     *                           strategy indicates otherwise or the thread is interrupted.
     * @param listeners          collection of retry listeners
     */
    public Retryer(@NotNull AttemptTimeLimiter<V> attemptTimeLimiter,
                   @NotNull StopStrategy stopStrategy,
                   @NotNull WaitStrategy waitStrategy,
                   @NotNull BlockStrategy blockStrategy,
                   @NotNull Predicate<Attempt<V>> rejectionPredicate,
                   @NotNull Collection<RetryListener<V>> listeners) {
        Assert.notNull(attemptTimeLimiter, "timeLimiter may not be null");
        Assert.notNull(stopStrategy, "stopStrategy may not be null");
        Assert.notNull(waitStrategy, "waitStrategy may not be null");
        Assert.notNull(blockStrategy, "blockStrategy may not be null");
        Assert.notNull(rejectionPredicate, "rejectionPredicate may not be null");
        Assert.notNull(listeners, "listeners may not null");

        this.attemptTimeLimiter = attemptTimeLimiter;
        this.stopStrategy = stopStrategy;
        this.waitStrategy = waitStrategy;
        this.blockStrategy = blockStrategy;
        this.rejectionPredicate = rejectionPredicate;
        this.listeners = listeners;
    }

    /**
     * Executes the given callable. If the rejection predicate
     * accepts the attempt, the stop strategy is used to decide if a new attempt
     * must be made. Then the wait strategy is used to decide how much time to sleep
     * and a new attempt is made.
     *
     * @param callable the callable task to be executed
     * @return the computed result of the given callable
     * @throws ExecutionException if the given callable throws an exception, and the
     *                            rejection predicate considers the attempt as successful. The original exception
     *                            is wrapped into an ExecutionException.
     * @throws RetryException     if all the attempts failed before the stop strategy decided
     *                            to abort, or the thread was interrupted. Note that if the thread is interrupted,
     *                            this exception is thrown and the thread's interrupt status is set.
     */
    public V call(Callable<V> callable) throws ExecutionException, RetryException {
        long startTime = System.nanoTime();
        for (int attemptNumber = 1; ; attemptNumber++) {
            Attempt<V> attempt;
            try {
                V result = attemptTimeLimiter.call(callable);
                attempt = new ResultAttempt<>(result, attemptNumber, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            } catch (Throwable t) {
                attempt = new ExceptionAttempt<>(t, attemptNumber, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            }

            for (RetryListener<V> listener : listeners) {
                listener.onRetry(attempt);
            }

            if (!rejectionPredicate.test(attempt)) {
                return attempt.get();
            }
            if (stopStrategy.shouldStop(attempt)) {
                throw new RetryException(attemptNumber, attempt);
            } else {
                long sleepTime = waitStrategy.computeSleepTime(attempt);
                try {
                    blockStrategy.block(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RetryException(attemptNumber, attempt);
                }
            }
        }
    }

    /**
     * Wraps the given {@link Callable} in a {@link RetryerCallable}, which can
     * be submitted to an executor. The returned {@link RetryerCallable} uses
     * this {@link Retryer} instance to call the given {@link Callable}.
     *
     * @param callable the callable to wrap
     * @return a {@link RetryerCallable} that behaves like the given {@link Callable}
     * with retry behavior defined by this {@link Retryer}
     */
    public RetryerCallable<V> wrap(Callable<V> callable) {
        return new RetryerCallable<>(this, callable);
    }

    @Immutable
    static final class ResultAttempt<R> implements Attempt<R> {
        private final R result;
        private final long attemptNumber;
        private final long delaySinceFirstAttempt;

        public ResultAttempt(R result, long attemptNumber, long delaySinceFirstAttempt) {
            this.result = result;
            this.attemptNumber = attemptNumber;
            this.delaySinceFirstAttempt = delaySinceFirstAttempt;
        }

        @Override
        public R get() throws ExecutionException {
            return result;
        }

        @Override
        public boolean hasResult() {
            return true;
        }

        @Override
        public boolean hasException() {
            return false;
        }

        @Override
        public R getResult() throws IllegalStateException {
            return result;
        }

        @Override
        public Throwable getExceptionCause() throws IllegalStateException {
            throw new IllegalStateException("The attempt resulted in a result, not in an exception");
        }

        @Override
        public long getAttemptNumber() {
            return attemptNumber;
        }

        @Override
        public long getDelaySinceFirstAttempt() {
            return delaySinceFirstAttempt;
        }
    }

    @Immutable
    static final class ExceptionAttempt<R> implements Attempt<R> {
        private final ExecutionException e;
        private final long attemptNumber;
        private final long delaySinceFirstAttempt;

        public ExceptionAttempt(Throwable cause, long attemptNumber, long delaySinceFirstAttempt) {
            this.e = new ExecutionException(cause);
            this.attemptNumber = attemptNumber;
            this.delaySinceFirstAttempt = delaySinceFirstAttempt;
        }

        @Override
        public R get() throws ExecutionException {
            throw e;
        }

        @Override
        public boolean hasResult() {
            return false;
        }

        @Override
        public boolean hasException() {
            return true;
        }

        @Override
        public R getResult() throws IllegalStateException {
            throw new IllegalStateException("The attempt resulted in an exception, not in a result");
        }

        @Override
        public Throwable getExceptionCause() throws IllegalStateException {
            return e.getCause();
        }

        @Override
        public long getAttemptNumber() {
            return attemptNumber;
        }

        @Override
        public long getDelaySinceFirstAttempt() {
            return delaySinceFirstAttempt;
        }
    }

    /**
     * A {@link Callable} which wraps another {@link Callable} in order to add
     * retrying behavior from a given {@link Retryer} instance.
     *
     * @author JB
     */
    public static class RetryerCallable<X> implements Callable<X> {
        private final Retryer<X> retryer;
        private final Callable<X> callable;

        private RetryerCallable(Retryer<X> retryer,
                                Callable<X> callable) {
            this.retryer = retryer;
            this.callable = callable;
        }

        /**
         * Makes the enclosing retryer call the wrapped callable.
         *
         * @see Retryer#call(Callable)
         */
        @Override
        public X call() throws ExecutionException, RetryException {
            return retryer.call(callable);
        }
    }
}

