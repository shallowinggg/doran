package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.IllegalConnectionUriException;
import com.shallowinggg.doran.client.RetryCountExhaustedException;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.common.util.retry.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author shallowinggg
 */
public class ConnectionFactoryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFactoryCache.class);
    private static final ConnectionFactoryCache INSTANCE = new ConnectionFactoryCache();
    private final Map<MQConfigInner, com.rabbitmq.client.Connection> rabbitMQCache = new ConcurrentHashMap<>();
    private final Map<MQConfigInner, javax.jms.Connection> activeMQCache = new ConcurrentHashMap<>();

    private final Retryer<Void> buildConnectionRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(3, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .withWaitStrategy(WaitStrategies.fibonacciWait())
            .build();

    public static ConnectionFactoryCache getInstance() {
        return INSTANCE;
    }

    public com.rabbitmq.client.Connection getRabbitMQConnection(MQConfig config) {
        MQConfigInner inner = new MQConfigInner(config);
        if (rabbitMQCache.containsKey(inner)) {
            return rabbitMQCache.get(inner);
        }

        synchronized (config.getName().intern()) {
            if (rabbitMQCache.containsKey(inner)) {
                return rabbitMQCache.get(inner);
            }
            final String uri = config.getUri();
            try {
                buildConnectionRetryer.call(() -> {
                    try {
                        com.rabbitmq.client.ConnectionFactory connectionFactory = new com.rabbitmq.client.ConnectionFactory();
                        connectionFactory.setUri(uri);
                        com.rabbitmq.client.Connection connection = connectionFactory.newConnection();
                        rabbitMQCache.put(inner, connection);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Create rabbitmq connection success, uri: {}", uri);
                        }
                        return null;
                    } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
                        throw new IllegalConnectionUriException(uri, e);
                    } catch (IOException | TimeoutException e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Create rabbitmq connection fail, uri: {}, retry it", uri, e);
                        }
                        throw e;
                    }
                });
            } catch (ExecutionException e) {
                // handle RuntimeException for IllegalConnectionUriException
                RuntimeException cause = (RuntimeException) e.getCause();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Create rabbitmq connection fail, uri: {}", uri, cause);
                }
                throw cause;
            } catch (RetryException e) {
                Attempt<?> attempt = e.getLastFailedAttempt();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Create rabbitmq connection fail, uri: {}, cause: retry count {} has exhausted",
                            uri, attempt.getAttemptNumber(), attempt.getExceptionCause());
                }
                throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
        return rabbitMQCache.get(inner);
    }

    public javax.jms.Connection getActiveMQConnection(MQConfig config) {
        MQConfigInner inner = new MQConfigInner(config);
        if (activeMQCache.containsKey(inner)) {
            return activeMQCache.get(inner);
        }

        final String uri = config.getUri();
        synchronized (config.getName().intern()) {
            if (activeMQCache.containsKey(inner)) {
                return activeMQCache.get(inner);
            }
            try {
                buildConnectionRetryer.call(() -> {
                    try {
                        String username = config.getUsername();
                        String password = config.getPassword();
                        javax.jms.ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(uri);
                        javax.jms.Connection connection = connectionFactory.createConnection(username, password);
                        activeMQCache.put(inner, connection);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Create activemq connection success, uri: {}", uri);
                        }
                        return null;
                    } catch (IllegalArgumentException e) {
                        throw new IllegalConnectionUriException(uri, e.getCause());
                    } catch (JMSException e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Create activemq connection fail, uri: {}, retry it", uri, e);
                        }
                        throw e;
                    }
                });
            } catch (ExecutionException e) {
                // handle RuntimeException for IllegalConnectionUriException
                RuntimeException cause = (RuntimeException) e.getCause();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Create activemq connection fail, uri: {}", uri, cause);
                }
                throw cause;
            } catch (RetryException e) {
                Attempt<?> attempt = e.getLastFailedAttempt();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Create activemq connection fail, uri: {}, cause: retry count {} has exhausted",
                            uri, attempt.getAttemptNumber(), attempt.getExceptionCause());
                }
                throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
        return activeMQCache.get(inner);
    }

    private static class MQConfigInner {
        private final MQType type;
        private final String uri;
        private final String username;
        private final String password;

        MQConfigInner(MQConfig config) {
            this.type = config.getType();
            this.uri = config.getUri();
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
            return type == inner.type &&
                    uri.equals(inner.uri) &&
                    username.equals(inner.username) &&
                    password.equals(inner.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, uri, username, password);
        }
    }
}
