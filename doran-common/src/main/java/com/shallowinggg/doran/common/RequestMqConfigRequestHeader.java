package com.shallowinggg.doran.common;

import com.shallowinggg.doran.common.util.StringUtils;
import com.shallowinggg.doran.transport.CommandCustomHeader;
import com.shallowinggg.doran.transport.annotation.CFNotNull;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;

/**
 * @author shallowinggg
 */
public class RequestMqConfigRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String configName;

    @Override
    public void checkFields() throws RemotingCommandException {
        if(!StringUtils.hasText(configName)) {
            throw new RemotingCommandException("configName must has text");
        }
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }
}
