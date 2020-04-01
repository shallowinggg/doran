package com.shallowinggg.doran.common;

import com.shallowinggg.doran.common.util.Assert;

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
public abstract class MQConfig implements Cloneable {

    public static final String DELIMITER = ",";

    /**
     * The name of MQ Config
     */
    private String name;

    /**
     * The message middleware to use
     */
    private MQType type;

    /**
     * MQ server uris, you can specify one or more uri and
     * separate them by {@link #DELIMITER}.
     * e.g. "amqp://userName:password@ipAddress:portNumber/virtualHos".
     * <p>
     * If you use ActiveMQ, you can specify broker urls like
     * "failover:url1, url2" directly, and thus use its special
     * features.
     */
    private String uri;

    /**
     * Represent how many threads will be used to send or receive
     * message. Default value is 1.
     */
    private int threadNum = 1;

    /**
     * Timestamp represents when the topic is created or updated.
     * It can be used to compare if the topic has been changed.
     */
    private long timestamp;

    /**
     * Special equals method that ignores {@link #threadNum}
     * field.
     *
     * @param other the another object to compare
     * @return {@code true} if all field's value are equals
     * except {@link #threadNum}, otherwise return false.
     */
    public abstract boolean equalsIgnoreThreadNum(MQConfig other);


    public boolean isChanged(long other) {
        return this.timestamp != other;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        Assert.notNull(name);
        this.name = name;
    }

    public MQType getType() {
        return type;
    }

    public void setType(MQType type) {
        Assert.notNull(type);
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        Assert.notNull(uri);
        this.uri = uri;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MQConfig{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", uri='" + uri + '\'' +
                ", threadNum=" + threadNum +
                ", timestamp=" + timestamp +
                '}';
    }
}
