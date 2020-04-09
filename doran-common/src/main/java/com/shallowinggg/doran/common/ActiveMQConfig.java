package com.shallowinggg.doran.common;

import com.shallowinggg.doran.common.util.Assert;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Special config for ActiveMQ.
 *
 * @author shallowinggg
 */
public class ActiveMQConfig extends MQConfig {

    /**
     * MQ server username
     */
    private String username;

    /**
     * MQ server password
     */
    private String password;

    /**
     * The name of the destination
     */
    private String destinationName;

    /**
     * The type of the destination
     */
    private DestinationType destinationType;


    // for consumer

    @Nullable
    private String clientId;

    @Nullable
    private String selector;

    public ActiveMQConfig() {
        super(MQType.ActiveMQ);
    }

    @Override
    public boolean equalsIgnoreThreadNum(MQConfig other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ActiveMQConfig that = (ActiveMQConfig) other;
        if (getThreadNum() != that.getThreadNum()) {
            return false;
        }

        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getUri(), that.getUri()) &&
                username.equals(that.username) &&
                password.equals(that.password) &&
                destinationName.equals(that.destinationName) &&
                destinationType == that.destinationType &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(selector, that.selector);
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

    public enum DestinationType {
        /**
         * PTP mode
         */
        PTP,
        /**
         * Topic mode
         */
        TOPIC
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        Assert.notNull(destinationName);
        this.destinationName = destinationName;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        Assert.notNull(destinationType);
        this.destinationType = destinationType;
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }

    public void setClientId(@Nullable String clientId) {
        this.clientId = clientId;
    }

    @Nullable
    public String getSelector() {
        return selector;
    }

    public void setSelector(@Nullable String selector) {
        this.selector = selector;
    }

    @Override
    public String toString() {
        return "ActiveMQConfig{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", destinationType=" + destinationType +
                ", clientId='" + clientId + '\'' +
                ", selector='" + selector + '\'' +
                "} " + super.toString();
    }


}
