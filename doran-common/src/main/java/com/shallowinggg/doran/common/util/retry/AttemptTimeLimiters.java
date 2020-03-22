package com.shallowinggg.doran.common.util.retry;

import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.concurrent.DirectExecutorService;
import com.shallowinggg.doran.common.util.concurrent.SimpleTimeLimiter;
import com.shallowinggg.doran.common.util.concurrent.TimeLimiter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Factory class for instances of {@link AttemptTimeLimiter}
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class AttemptTimeLimiters {

    private AttemptTimeLimiters() {
    }

    /**
     * @param <V> The type of the computation result.
     * @return an {@link AttemptTimeLimiter} impl which has no time limit
     */
    public static <V> AttemptTimeLimiter<V> noTimeLimit() {
        return new AttemptTimeLimiters.NoAttemptTimeLimit<>();
    }

    /**
     * For control over thread management, it is preferable to offer an {@link ExecutorService} through the other
     * factory method, {@link #fixedTimeLimit(long, TimeUnit, ExecutorService)}. See the note on
     * {@link SimpleTimeLimiter#create(ExecutorService)}, which this AttemptTimeLimiter uses.
     *
     * @param duration that an attempt may persist before being circumvented
     * @param timeUnit of the 'duration' arg
     * @param <V>      the type of the computation result
     * @return an {@link AttemptTimeLimiter} with a fixed time limit for each attempt
     */
    public static <V> AttemptTimeLimiter<V> fixedTimeLimit(long duration, @NotNull TimeUnit timeUnit) {
        Assert.notNull(timeUnit);
        return new FixedAttemptTimeLimit<>(duration, timeUnit);
    }

    /**
     * @param duration        that an attempt may persist before being circumvented
     * @param timeUnit        of the 'duration' arg
     * @param executorService used to enforce time limit
     * @param <V>             the type of the computation result
     * @return an {@link AttemptTimeLimiter} with a fixed time limit for each attempt
     */
    public static <V> AttemptTimeLimiter<V> fixedTimeLimit(long duration, @NotNull TimeUnit timeUnit,
                                                           @NotNull ExecutorService executorService) {
        Assert.notNull(timeUnit);
        return new FixedAttemptTimeLimit<>(duration, timeUnit, executorService);
    }

    @Immutable
    private static final class NoAttemptTimeLimit<V> implements AttemptTimeLimiter<V> {
        @Override
        public V call(Callable<V> callable) throws Exception {
            return callable.call();
        }
    }

    @Immutable
    private static final class FixedAttemptTimeLimit<V> implements AttemptTimeLimiter<V> {

        private final TimeLimiter timeLimiter;
        private final long duration;
        private final TimeUnit timeUnit;

        public FixedAttemptTimeLimit(long duration, @NotNull TimeUnit timeUnit) {
            this(SimpleTimeLimiter.create(new DirectExecutorService()), duration, timeUnit);
        }

        public FixedAttemptTimeLimit(long duration, @NotNull TimeUnit timeUnit, @NotNull ExecutorService executorService) {
            this(SimpleTimeLimiter.create(executorService), duration, timeUnit);
        }

        private FixedAttemptTimeLimit(@NotNull TimeLimiter timeLimiter, long duration, @NotNull TimeUnit timeUnit) {
            Assert.notNull(timeLimiter);
            Assert.notNull(timeUnit);
            this.timeLimiter = timeLimiter;
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        @Override
        public V call(Callable<V> callable) throws Exception {
            return timeLimiter.callWithTimeout(callable, duration, timeUnit);
        }
    }
}
