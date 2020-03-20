package com.shallowinggg.doran.common;

/**
 * @author shallowinggg
 */
public enum MQType {
    /**
     * RabbitMQ
     */
    RabbitMQ("RabbitMQ"),
    /**
     * ActiveMQ
     */
    ActiveMQ("ActiveMQ"),
    /**
     * Unknown MQ type
     */
    UNKNOWN("Unknown");

    private final String type;

    MQType(String type) {
        this.type = type;
    }

    public static MQType parse(String val) {
        for (MQType type : MQType.values()) {
            if (type.type.equals(val)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
