package com.shallowinggg.doran.common;

/**
 * @author shallowinggg
 */
public class EmptyMQConfig extends MQConfig {

    public EmptyMQConfig() {
        super("empty", MQType.UNKNOWN, "empty", "empty", "empty", 0);
    }

    @Override
    public String extFieldsToJson() {
        return null;
    }
}
