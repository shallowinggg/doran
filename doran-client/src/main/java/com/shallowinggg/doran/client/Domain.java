package com.shallowinggg.doran.client;

/**
 * @author shallowinggg
 */

public enum Domain {
    /**
     * Point To Point mode, aka, message sent will only
     * be consumed by one consumer.
     */
    PTP("PTP"),
    /**
     * Topic mode, aka, message sent will be consumed by
     * one or more consumer.
     */
    TOPIC("TOPIC");

    private final String type;

    Domain(String type) {
        this.type = type;
    }

    public static Domain parse(String type) {
        for(Domain domain : values()) {
            if(domain.type.equals(type)) {
                return domain;
            }
        }
        return null;
    }

    public String getType() {
        return type;
    }
}
