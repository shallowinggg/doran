package com.shallowinggg.doran.server.web.entity;

import com.shallowinggg.doran.common.MQType;

/**
 * Entity class that represents active mq infos.
 *
 * @author shallowinggg
 */
public class ActiveConfig {
    private final String name;
    private final MQType type;

    public ActiveConfig(String name, MQType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public MQType getType() {
        return type;
    }
}
