package com.shallowinggg.doran.common.util.retry;

import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

/**
 * Factory class for {@link StopStrategy} instances.
 *
 * @author JB
 */
public final class StopStrategies {
    private static final StopStrategy NEVER_STOP = new NeverStopStrategy();

    private StopStrategies() {
    }

    /**
     * Returns a stop strategy which never stops retrying. It might be best to
     * try not to abuse services with this kind of behavior when small wait
     * intervals between retry attempts are being used.
     *
     * @return a stop strategy which never stops
     */
    public static StopStrategy neverStop() {
        return NEVER_STOP;
    }

    /**
     * Returns a stop strategy which stops after N failed attempts.
     *
     * @param attemptNumber the number of failed attempts before stopping
     * @return a stop strategy which stops after {@code attemptNumber} attempts
     */
    public static StopStrategy stopAfterAttempt(int attemptNumber) {
        return new StopAfterAttemptStrategy(attemptNumber);
    }

    /**
     * Returns a stop strategy which stops after a given delay. If an
     * unsuccessful attempt is made, this {@link StopStrategy} will check if the
     * amount of time that's passed from the first attempt has exceeded the
     * given delay amount. If it has exceeded this delay, then using this
     * strategy causes the retrying to stop.
     *
     * @param delayInMillis the delay, in milliseconds, starting from first attempt
     * @return a stop strategy which stops after {@code delayInMillis} time in milliseconds
     * @deprecated Use {@link #stopAfterDelay(long, TimeUnit)} instead.
     */
    @Deprecated
    public static StopStrategy stopAfterDelay(long delayInMillis) {
        return stopAfterDelay(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns a stop strategy which stops after a given delay. If an
     * unsuccessful attempt is made, this {@link StopStrategy} will check if the
     * amount of time that's passed from the first attempt has exceeded the
     * given delay amount. If it has exceeded this delay, then using this
     * strategy causes the retrying to stop.
     *
     * @param duration the delay, starting from first attempt
     * @param timeUnit the unit of the duration
     * @return a stop strategy which stops after {@code delayInMillis} time in milliseconds
     */
    public static StopStrategy stopAfterDelay(long duration, @NotNull TimeUnit timeUnit) {
        Assert.notNull(timeUnit, "The time unit may not be null");
        return new StopAfterDelayStrategy(timeUnit.toMillis(duration));
    }

    @Immutable
    private static final class NeverStopStrategy implements StopStrategy {
        @Override
        public <V> boolean shouldStop(Attempt<V> failedAttempt) {
            return false;
        }
    }

    @Immutable
    private static final class StopAfterAttemptStrategy implements StopStrategy {
        private final int maxAttemptNumber;

        public StopAfterAttemptStrategy(int maxAttemptNumber) {
            Assert.isTrue(maxAttemptNumber >= 1, "maxAttemptNumber must be >= 1 but is " + maxAttemptNumber);
            this.maxAttemptNumber = maxAttemptNumber;
        }

        @Override
        public <V> boolean shouldStop(Attempt<V> failedAttempt) {
            return failedAttempt.getAttemptNumber() >= maxAttemptNumber;
        }
    }

    @Immutable
    private static final class StopAfterDelayStrategy implements StopStrategy {
        private final long maxDelay;

        public StopAfterDelayStrategy(long maxDelay) {
            Assert.isTrue(maxDelay >= 0L, "maxDelay must be >= 0 but is " + maxDelay);
            this.maxDelay = maxDelay;
        }

        @Override
        public <V> boolean shouldStop(Attempt<V> failedAttempt) {
            return failedAttempt.getDelaySinceFirstAttempt() >= maxDelay;
        }
    }
}

