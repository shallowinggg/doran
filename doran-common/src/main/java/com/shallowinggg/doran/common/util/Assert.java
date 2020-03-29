package com.shallowinggg.doran.common.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.Nullable;

/**
 * @author shallowinggg
 */
public abstract class Assert {
    /**
     * Assert that an object is not {@code null}.
     */
    @CanIgnoreReturnValue
    public static <T> T notNull(@Nullable T o) {
        return notNull(o, "[Assertion failed] - this argument is required; it must not be null");
    }

    /**
     * Assert that an object is not {@code null}.
     * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
     *
     * @param o   the object to check
     * @param msg the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null}
     */
    @CanIgnoreReturnValue
    public static <T> T notNull(@Nullable T o, String msg) {
        if (o == null) {
            throw new IllegalArgumentException(msg);
        }
        return o;
    }

    public static void isTrue(boolean expr, String msg) {
        if (!expr) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Assert a boolean expression, throwing an {@code IllegalStateException}
     * if the expression evaluates to {@code false}.
     * <p>Call {@link #isTrue} if you wish to throw an {@code IllegalArgumentException}
     * on an assertion failure.
     * <pre class="code">Assert.state(id == null, "The id property must not already be initialized");</pre>
     * @param expression a boolean expression
     * @param message the exception message to use if the assertion fails
     * @throws IllegalStateException if {@code expression} is {@code false}
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public static void equals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected: " + expected + ", actual: " + actual);
        }
    }

    public static void equals(double expected, double actual, double delta) {
        if (Double.compare(expected, actual) == 0) {
            return;
        }
        if ((Math.abs(expected - actual) <= delta)) {
            return;
        }
        throw new AssertionError("expected: " + expected + ", actual: " + actual);
    }

    public static void equals(String expected, String actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected != null && !expected.equals(actual) || !actual.equals(expected)) {
            throw new AssertionError("expected: " + expected + ", actual: " + actual);
        }
    }

    public static void hasText(CharSequence c) {
        if (!StringUtils.hasText(c)) {
            throw new IllegalArgumentException("[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
        }
    }

    public static void hasText(CharSequence c, String message) {
        if (!StringUtils.hasText(c)) {
            throw new IllegalArgumentException(message);
        }
    }
}
