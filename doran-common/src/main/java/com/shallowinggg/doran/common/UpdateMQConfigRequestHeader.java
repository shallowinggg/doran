package com.shallowinggg.doran.common;

import com.shallowinggg.doran.transport.CommandCustomHeader;
import com.shallowinggg.doran.transport.annotation.CFNotNull;
import com.shallowinggg.doran.transport.exception.RemotingCommandException;

/**
 * @author shallowinggg
 */
public class UpdateMQConfigRequestHeader implements CommandCustomHeader {

    @CFNotNull
    private String name;

    @CFNotNull
    private String type;

    @CFNotNull
    private String uri;

    @CFNotNull
    private String username;

    @CFNotNull
    private String password;

    @CFNotNull
    private int threadNum;

    @CFNotNull
    private long timestamp;

    @CFNotNull
    private String extFieldsJson;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getExtFieldsJson() {
        return extFieldsJson;
    }

    public void setExtFieldsJson(String extFieldsJson) {
        this.extFieldsJson = extFieldsJson;
    }
}
