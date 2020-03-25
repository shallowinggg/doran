package com.shallowinggg.doran.client.producer;

import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.shallowinggg.doran.client.IllegalConnectionUriException;
import com.shallowinggg.doran.client.RetryCountExhaustedException;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.StringUtils;
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

    /**
     * uri -> ConnectionFactory
     */
    private final Map<String, com.rabbitmq.client.ConnectionFactory> rabbitMQConnectionFactories
            = new ConcurrentHashMap<>();
    /**
     * config name -> Connection
     */
    private final Map<String, com.rabbitmq.client.Connection> rabbitMQCache = new ConcurrentHashMap<>();
    private final Map<MQConfigInner, javax.jms.Connection> activeMQCache = new ConcurrentHashMap<>();
    // TODO: 缓存ConnectionFactory，避免共用一条连接压力过大

    private final Retryer<Void> buildConnectionRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(3, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .withWaitStrategy(WaitStrategies.fibonacciWait())
            .build();

    public static ConnectionFactoryCache getInstance() {
        return INSTANCE;
    }

    public com.rabbitmq.client.ConnectionFactory getRabbitMQConnectionFactory(String uri) {
        Assert.isTrue(StringUtils.hasText(uri), "'uri' must has text");
        if (rabbitMQConnectionFactories.containsKey(uri)) {
            return rabbitMQConnectionFactories.get(uri);
        }
        synchronized (uri.intern()) {
            if (rabbitMQConnectionFactories.containsKey(uri)) {
                return rabbitMQConnectionFactories.get(uri);
            }

            try {
                com.rabbitmq.client.ConnectionFactory connectionFactory = new com.rabbitmq.client.ConnectionFactory();
                connectionFactory.setUri(uri);
                connectionFactory.setAutomaticRecoveryEnabled(true);
                // TODO: 可配置
                connectionFactory.setNetworkRecoveryInterval(1000);
                connectionFactory.setRequestedHeartbeat(3);
                rabbitMQConnectionFactories.put(uri, connectionFactory);
                return connectionFactory;
            } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
                throw new IllegalConnectionUriException(uri, e);
            }
        }
    }

    public com.rabbitmq.client.Connection getRabbitMQConnection(MQConfig config) {
        String name = config.getName();
        if (rabbitMQCache.containsKey(name)) {
            return rabbitMQCache.get(name);
        }

        synchronized (config.getName().intern()) {
            if (rabbitMQCache.containsKey(name)) {
                return rabbitMQCache.get(name);
            }
            final String uri = config.getUri();
            com.rabbitmq.client.ConnectionFactory connectionFactory = getRabbitMQConnectionFactory(uri);
            try {
                buildConnectionRetryer.call(() -> {
                    try {
                        com.rabbitmq.client.Connection connection = connectionFactory.newConnection();
                        AutorecoveringConnection recoverConnection = (AutorecoveringConnection) connection;
                        recoverConnection.addRecoveryListener(new RecoveryListener() {
                            @Override
                            public void handleRecovery(Recoverable recoverable) {
                                if(LOGGER.isErrorEnabled()) {
                                    LOGGER.error("Connection {} lost connection", recoverable);
                                }
                            }

                            @Override
                            public void handleRecoveryStarted(Recoverable recoverable) {
                                if(LOGGER.isInfoEnabled()) {
                                    LOGGER.info("Connection {} recover success", recoverable);
                                }
                            }
                        });
                        rabbitMQCache.put(name, connection);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Create rabbitmq connection success, uri: {}", uri);
                        }
                        return null;
                    } catch (IOException | TimeoutException e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Create rabbitmq connection fail, uri: {}, retry", uri, e);
                        }
                        throw e;
                    }
                });
            } catch (ExecutionException e) {
                // won't goto this branch
                assert false;
            } catch (RetryException e) {
                Attempt<?> attempt = e.getLastFailedAttempt();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Create rabbitmq connection fail, uri: {}, retry count {} has exhausted",
                            uri, attempt.getAttemptNumber(), attempt.getExceptionCause());
                }
                throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
        return rabbitMQCache.get(name);
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
                            LOGGER.warn("Create activemq connection fail, uri: {}, retry", uri, e);
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
                    LOGGER.error("Create activemq connection fail, uri: {}, retry count {} has exhausted",
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
