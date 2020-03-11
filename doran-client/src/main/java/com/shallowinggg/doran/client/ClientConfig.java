package com.shallowinggg.doran.client;

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

    /**
     * Core thread numbers for requesting MQ Configs. Actually,
     * when the system has warmed up, there will be few requests
     * for MQ Configs, this number should be small so as to
     * decrease system cost.
     */
    private int requestConfigThreadNum = 2;

    /**
     * Max thread numbers for request MQ Configs. This value should
     * be a few larger, in case many requests occur at the same
     * time. Otherwise, the latency will be too high.
     */
    private int requestConfigMaxThreadNum = 10;

    /**
     * Timeout in milliseconds for sync invokable.
     */
    private int timeoutMillis = 5000;

    private int retryTimesWhenNetworkFluctuation = 3;

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

    public int getRequestConfigThreadNum() {
        return requestConfigThreadNum;
    }

    public void setRequestConfigThreadNum(int requestConfigThreadNum) {
        this.requestConfigThreadNum = requestConfigThreadNum;
    }

    public int getRequestConfigMaxThreadNum() {
        return requestConfigMaxThreadNum;
    }

    public void setRequestConfigMaxThreadNum(int requestConfigMaxThreadNum) {
        this.requestConfigMaxThreadNum = requestConfigMaxThreadNum;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
