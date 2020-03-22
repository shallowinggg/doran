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
public abstract class MQConfig implements Cloneable {

    public static final String DELIMITER = ",";

    /**
     * The name of MQ Config
     */
    private final String name;

    /**
     * The Message middleware to use
     */
    private final MQType type;

    /**
     * MQ server uris, you can specify one or more uri and
     * separate them by {@link #DELIMITER}.
     * e.g. "amqp://userName:password@ipAddress:portNumber/virtualHos".
     * <p>
     * If you use ActiveMQ, you can specify broker urls like
     * "failover:url1, url2" directly, and thus use its special
     * features.
     */
    private final String uri;

    /**
     * MQ server username
     */
    private final String username;

    /**
     * MQ server password
     */
    private final String password;

    /**
     * Represent how many threads will be used to send or receive
     * message. Default value is 1.
     */
    private final int threadNum;

    /**
     * Timestamp represents when the topic is created or updated.
     * It can be used to compare if the topic has been changed.
     */
    private final long timestamp;

    protected MQConfig(String name, MQType type, String uri, String username, String password, long timestamp) {
        this(name, type, uri, username, password, 1, timestamp);
    }

    protected MQConfig(String name, MQType type, String uri, String username, String password, int threadNum, long timestamp) {
        this.name = name;
        this.type = type;
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.threadNum = threadNum;
        this.timestamp = timestamp;
    }

    /**
     * Sub class should implements this method and convert
     * their declared fields to json value.
     *
     * @return json for sub class's declared fields
     */
    public abstract String extFieldsToJson();

    public String getName() {
        return name;
    }

    public MQType getType() {
        return type;
    }

    public String getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isChanged(long other) {
        return this.timestamp != other;
    }

    public boolean equalsIgnoreThreadNum(MQConfig other) {
        if (other == null) {
            return false;
        }
        return name.equals(other.name) &&
                type == other.type &&
                uri.equals(other.uri) &&
                username.equals(other.username) &&
                password.equals(other.password);
    }

    @Override
    public String toString() {
        return "MQConfig{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", uri='" + uri + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", threadNum=" + threadNum +
                ", timestamp=" + timestamp +
                '}';
    }
}
