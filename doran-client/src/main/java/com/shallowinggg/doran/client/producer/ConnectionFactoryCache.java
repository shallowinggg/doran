package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.IllegalConnectionUriException;
import com.shallowinggg.doran.common.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author shallowinggg
 */
public class ConnectionFactoryCache {
    private static final ConnectionFactoryCache INSTANCE = new ConnectionFactoryCache();
    private final Map<MQConfigInner, com.rabbitmq.client.Connection> rabbitMQCache = new ConcurrentHashMap<>();
    private final Map<MQConfigInner, javax.jms.Connection> activeMQCache = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public static ConnectionFactoryCache getInstance() {
        return INSTANCE;
    }

    public com.rabbitmq.client.Connection getRabbitMQConnection(MqConfig config) {
        MQConfigInner inner = new MQConfigInner(config);
        if (rabbitMQCache.containsKey(inner)) {
            return rabbitMQCache.get(inner);
        }

        String urls = null;
        lock.lock();
        try {
            urls = config.getUrls();
            com.rabbitmq.client.ConnectionFactory connectionFactory = new com.rabbitmq.client.ConnectionFactory();
            connectionFactory.setUri(config.getUrls());
            com.rabbitmq.client.Connection connection = connectionFactory.newConnection();
            rabbitMQCache.put(inner, connection);
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            throw new IllegalConnectionUriException(urls, e);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return rabbitMQCache.get(inner);
    }

    public javax.jms.Connection getActiveMQConnection(MqConfig config) {
        MQConfigInner inner = new MQConfigInner(config);
        if (activeMQCache.containsKey(inner)) {
            return activeMQCache.get(inner);
        }

        String urls = null;
        lock.lock();
        try {
            urls = config.getUrls();
            String username = config.getUsername();
            String password = config.getPassword();
            javax.jms.ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(urls);
            javax.jms.Connection connection = connectionFactory.createConnection(username, password);
            activeMQCache.put(inner, connection);
        } catch (IllegalArgumentException e) {
            throw new IllegalConnectionUriException(urls, e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return activeMQCache.get(inner);
    }

    private static class MQConfigInner {
        private final String name;
        private final String uri;
        private final String username;
        private final String password;

        MQConfigInner(MqConfig config) {
            this.name = config.getName();
            this.uri = config.getUrls();
            this.username = config.getUsername();
            this.password = config.getPassword();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MQConfigInner inner = (MQConfigInner) o;
            return name.equals(inner.name) &&
                    uri.equals(inner.uri) &&
                    username.equals(inner.username) &&
                    password.equals(inner.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, uri, username, password);
        }
    }
}
