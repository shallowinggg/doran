package com.shallowinggg.doran.common;

import com.shallowinggg.doran.transport.CommandCustomHeader;
import com.shallowinggg.doran.transport.annotation.CFNotNull;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;

/**
 * @author shallowinggg
 */
public class RegisterClientResponseHeader implements CommandCustomHeader {

    @CFNotNull
    private Integer holdingMqConfigNums;

    @Override
    public void checkFields() throws RemotingCommandException {
        if(holdingMqConfigNums <0) {
            throw new RemotingCommandException("'holdingMqConfigNums' must not be negative");
        }
    }

    public void setHoldingMqConfigNums(Integer holdingMqConfigNums) {
        this.holdingMqConfigNums = holdingMqConfigNums;
    }

    public Integer getHoldingMqConfigNums() {
        return holdingMqConfigNums;
    }
}
