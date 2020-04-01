package com.shallowinggg.doran.common;

import com.shallowinggg.doran.transport.CommandCustomHeader;
import com.shallowinggg.doran.transport.annotation.CFNotNull;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;

/**
 * @author shallowinggg
 */
public class RequestMQConfigResponseHeader implements CommandCustomHeader {

    @CFNotNull
    private String type;

    @CFNotNull
    private String config;

    @Override
    public void checkFields() throws RemotingCommandException {
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
