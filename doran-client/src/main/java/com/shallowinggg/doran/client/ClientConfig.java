package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.util.SystemUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author shallowinggg
 */
public class ClientConfig {
    private String serverAddr;
    private String username;
    private String password;

    private String clientId = createClientId();

    private String clientName = localHostName();

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

    private static String localHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // do nothing
        }

        return "DEFAULT_CLIENT";
    }

    /**
     * Create the unique client id, format "ip:pid".
     *
     * @return client id
     */
    private static String createClientId() {
        byte[] ip = SystemUtils.getIp();
        if (ip == null) {
            ip = SystemUtils.createFakeIp();
        }

        int pid = SystemUtils.getPid();
        StringBuilder clientId = new StringBuilder(15 + 1 + 5);
        for (int i = 0, len = ip.length; i < len; ++i) {
            clientId.append(ip[i]);
            if (i != len - 1) {
                clientId.append(".");
            }
        }
        clientId.append(':').append(pid);
        return clientId.toString();
    }

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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
