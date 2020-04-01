package com.shallowinggg.doran.common;

/**
 * @author shallowinggg
 */
public class EmptyMQConfig extends MQConfig {

    public EmptyMQConfig() {
        setType(MQType.UNKNOWN);
    }

    @Override
    public boolean equalsIgnoreThreadNum(MQConfig other) {
        return false;
    }
}
