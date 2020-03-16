package com.shallowinggg.doran.client.resolver;

/**
 * Exception thrown when check inet address fail.
 *
 * @author shallowinggg
 * @see InetAddressChecker#check(String)
 */
public class InvalidInetAddressException extends RuntimeException {
    public InvalidInetAddressException(String msg) {
        super(msg);
    }

    public InvalidInetAddressException(Throwable cause) {
        super(cause);
    }
}
