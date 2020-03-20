package com.shallowinggg.doran.common;

/**
 * This class is provided to store basic MQ configurations.
 * It is retrieved by communicated with server, and sync
 * with server data.
 * <p>
 * Client producer and consumer should hold this config
 * and its timestamp. When this config is updated, client
 * can invoke {@link #isChanged(long)} method with time
 * stored before to check if this config has been updated.
 *
 * @author shallowinggg
 */
public class MqConfig implements Cloneable {

    public static final String DELIMITER = ",";

    /**
     * Unique config name
     */
    private String name;

    /**
     * MQ server urls, you can specify one or more urls and
     * separated by {@link #DELIMITER}.
     * e.g. "amqp://userName :password@ipAddress:portNumber/virtualHos".
     * <p>
     * If you use ActiveMQ, you can specify broker urls like
     * "failover:url1, url2" directly, and thus use its special
     * features.
     */
    private String urls;

    /**
     * MQ server username
     */
    private String username;

    /**
     * MQ server password
     */
    private String password;

    /**
     * Domain type, support PTP mode and Topic mode.
     *
     * @see Domain
     */
    private Domain domain;

    /**
     * Domain name, aka queue name or topic name.
     */
    private String domainName;

    /**
     * Represent how many threads will be used to send message
     * to this queue or topic, default is 1.
     */
    private int threadNum = 1;

    /**
     * Timestamp represents when the topic is created or updated.
     * It can be used to compare if the topic has been changed.
     */
    private long timestamp;


    /**
     * Provide a convenient way to copy from other config.
     * The name of the topic is unique, so it won't be copied.
     * <p>
     * When the topic is updated in server, this method will
     * be invoked.
     *
     * @param other another config
     */
    public void copyFrom(MqConfig other) {
        setUrls(other.getUrls());
        setUsername(other.getUsername());
        setPassword(other.getPassword());
        setDomain(other.getDomain());
        setDomainName(other.getDomainName());
        setThreadNum(other.getThreadNum());
        setTimestamp(other.getTimestamp());
    }

    public static MqConfig obtainFromMqConfigHeader(MqConfigHeader header) {
        MqConfig config = new MqConfig();
        config.setName(header.getName());
        config.setUrls(header.getUrls());
        config.setUsername(header.getUsername());
        config.setPassword(header.getPassword());
        config.setDomain(Domain.valueOf(header.getDomain()));
        config.setDomainName(header.getDomainName());
        config.setThreadNum(header.getThreadNum());
        config.setTimestamp(header.getTimestamp());
        return config;
    }

    public boolean isChanged(long lastUpdateTime) {
        return lastUpdateTime < this.timestamp;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
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

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public boolean equalsIgnoreThreadNum(MqConfig other) {
        if (other == null) {
            return false;
        }
        return name.equals(other.name) &&
                urls.equals(other.urls) &&
                username.equals(other.username) &&
                password.equals(other.password) &&
                domain.equals(other.domain) &&
                domainName.equals(other.domainName);
    }

    @Override
    public String toString() {
        return "MqConfig{" +
                "name='" + name + '\'' +
                ", urls='" + urls + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", domain=" + domain +
                ", domainName='" + domainName + '\'' +
                ", threadNum=" + threadNum +
                ", timestamp=" + timestamp +
                '}';
    }
}
