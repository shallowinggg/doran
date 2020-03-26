package com.shallowinggg.doran.client.common;

/**
 * @author shallowinggg
 */
public class RetryCountExhaustedException extends RuntimeException {
    public RetryCountExhaustedException(int retryCount, Throwable cause) {
        super("Retry count " + retryCount + " has exhausted", cause);
    }
}
