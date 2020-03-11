package com.shallowinggg.doran.common.exception;

/**
 * @author shallowinggg
 */
public class SystemException extends RuntimeException {
    public SystemException(String msg) {
        super(msg);
    }

    public SystemException(Throwable cause) {
        super(cause);
    }

    public SystemException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
