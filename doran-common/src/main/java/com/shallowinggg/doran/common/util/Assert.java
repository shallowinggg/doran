package com.shallowinggg.doran.common.util;

/**
 * @author shallowinggg
 */
public class Assert {

    private Assert() {
    }

    public static void notNull(Object o, String msg) {
        if (o == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void isTrue(boolean expr, String msg) {
        if (!expr) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void equals(int expected, int actual) {
        if(expected != actual) {
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
        if(expected == null && actual == null) {
            return;
        }

        if(expected != null && !expected.equals(actual) || !actual.equals(expected)) {
            throw new AssertionError("expected: " + expected + ", actual: " + actual);
        }
    }

    public static void hasText(CharSequence c) {
        if(!StringUtils.hasText(c)) {
            throw new IllegalArgumentException("[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
        }
    }
}
