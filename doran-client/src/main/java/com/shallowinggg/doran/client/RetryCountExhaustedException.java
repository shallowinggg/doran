package com.shallowinggg.doran.client;

/**
 * @author shallowinggg
 */
public class RetryCountExhaustedException extends RuntimeException {
    public RetryCountExhaustedException(int retryCount, Throwable cause) {
        super("Retry count " + retryCount + " has exhausted", cause);
    }
}
