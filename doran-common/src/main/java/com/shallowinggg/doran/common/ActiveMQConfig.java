package com.shallowinggg.doran.common;

/**
 * Special config for ActiveMQ.
 *
 * @author shallowinggg
 */
public class ActiveMQConfig extends MQConfig {

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                          long timestamp, String json) {
        super(name, type, uri, username, password, threadNum, timestamp);

    }

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, long timestamp) {
        super(name, type, uri, username, password, timestamp);
    }

    public ActiveMQConfig(String name, MQType type, String uri, String username, String password, int threadNum,
                             long timestamp) {
        super(name, type, uri, username, password, threadNum, timestamp);
    }

    @Override
    public String extFieldsToJson() {
        return null;
    }


}
