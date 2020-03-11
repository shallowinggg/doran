package com.shallowinggg.doran.common.exception;

/**
 * @author shallowinggg
 */
public class ConfigNotExistException extends RuntimeException {
    private final String configName;

    public ConfigNotExistException(String configName) {
        super();
        this.configName = configName;
    }

    public ConfigNotExistException(String configName, Throwable cause) {
        super(cause);
        this.configName = configName;
    }

    @Override
    public String getMessage() {
        return "Mq config " + configName + " is not exist in server";
    }
}
