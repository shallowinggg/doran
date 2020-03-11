package com.shallowinggg.doran.common.exception;

/**
 * @author shallowinggg
 */
public class UnexpectedResponseException extends SystemException {

    public UnexpectedResponseException(int responseCode, String request) {
        super("unexpected response code: " + responseCode + " for request: " + request);
    }

    public UnexpectedResponseException(int responseCode, String request, Throwable cause) {
        super("unexpected response code: " + responseCode + " for request: " + request, cause);
    }

}
