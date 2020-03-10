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
    private String topicName;

    @Override
    public void checkFields() throws RemotingCommandException {
        if(StringUtils.hasText(topicName)) {
            throw new RemotingCommandException("topicName must has text");
        }
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }
}
