package com.shallowinggg.doran.server.web.entity;

import com.shallowinggg.doran.common.MQType;

/**
 * Entity class that represents active mq infos.
 *
 * @author shallowinggg
 */
public class ActiveConfig {
    private String name;
    private MQType type;
    private long updateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MQType getType() {
        return type;
    }

    public void setType(MQType type) {
        this.type = type;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
