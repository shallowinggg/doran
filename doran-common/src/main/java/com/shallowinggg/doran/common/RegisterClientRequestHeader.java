package com.shallowinggg.doran.common;

import com.shallowinggg.doran.transport.CommandCustomHeader;
import com.shallowinggg.doran.transport.annotation.CFNotNull;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;

/**
 * @author shallowinggg
 */
public class RegisterClientRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String clientId;

    @CFNotNull
    private String clientName;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
