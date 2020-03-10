package com.shallowinggg.doran.client;

import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ClientConfig {
    private String serverAddr;
    private String username;
    private String password;

    /**
     * Heartbeat interval in milliseconds with server
     */
    private int heartBeatServerInterval = 1000 * 30;

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
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

    public int getHeartBeatServerInterval() {
        return heartBeatServerInterval;
    }

    public void setHeartBeatServerInterval(int heartBeatServerInterval) {
        this.heartBeatServerInterval = heartBeatServerInterval;
    }
}
