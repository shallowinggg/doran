package com.shallowinggg.doran.client;

/**
 * @author shallowinggg
 */
public class IllegalConnectionUriException extends RuntimeException {
    public IllegalConnectionUriException(String uri, Throwable cause) {
        super("Illegal URI: " + uri, cause);
    }
}
