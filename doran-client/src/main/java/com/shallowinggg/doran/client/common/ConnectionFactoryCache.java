package com.shallowinggg.doran.client.common;

import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.StringUtils;
import com.shallowinggg.doran.common.util.retry.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    /**
     * uri -> ConnectionFactory
     */
    private final Map<String, javax.jms.ConnectionFactory> activeMQConnectionFactories
            = new ConcurrentHashMap<>();
    /**
     * config name -> Connection
     */
    private final Map<String, javax.jms.Connection> activeMQCache = new ConcurrentHashMap<>();

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
                    com.rabbitmq.client.Connection connection = connectionFactory.newConnection();
                    AutorecoveringConnection recoverConnection = (AutorecoveringConnection) connection;
                    recoverConnection.addRecoveryListener(new RecoveryListener() {
                        @Override
                        public void handleRecovery(Recoverable recoverable) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("Connection {} lost connection", recoverable);
                            }
                        }

                        @Override
                        public void handleRecoveryStarted(Recoverable recoverable) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Connection {} recover success", recoverable);
                            }
                        }
                    });
                    rabbitMQCache.put(name, connection);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Create rabbitmq connection success, uri: {}", uri);
                    }
                    return null;
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

    public javax.jms.ConnectionFactory getActiveMQConnectionFactory(String uri) {
        Assert.isTrue(StringUtils.hasText(uri), "'uri' must has text");
        if (activeMQConnectionFactories.containsKey(uri)) {
            return activeMQConnectionFactories.get(uri);
        }
        synchronized (uri.intern()) {
            if (activeMQConnectionFactories.containsKey(uri)) {
                return activeMQConnectionFactories.get(uri);
            }

            try {
                javax.jms.ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(uri);
                activeMQConnectionFactories.put(uri, connectionFactory);
                return connectionFactory;
            } catch (IllegalArgumentException e) {
                throw new IllegalConnectionUriException(uri, e.getCause());
            }
        }
    }

    public javax.jms.Connection getActiveMQConnection(MQConfig config, @Nullable String clientId) {
        final String name = config.getName();
        if (activeMQCache.containsKey(name)) {
            return activeMQCache.get(name);
        }

        final String uri = config.getUri();
        javax.jms.ConnectionFactory factory = getActiveMQConnectionFactory(uri);
        synchronized (name.intern()) {
            if (activeMQCache.containsKey(name)) {
                return activeMQCache.get(name);
            }
            try {
                buildConnectionRetryer.call(() -> {
                    String username = config.getUsername();
                    String password = config.getPassword();
                    javax.jms.Connection connection = factory.createConnection(username, password);
                    if(clientId != null) {
                        connection.setClientID(clientId);
                    }
                    connection.start();
                    connection.setExceptionListener(e -> {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("ActiveMQ connection fail", e);
                        }
                    });
                    activeMQCache.put(name, connection);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Create activemq connection success, uri: {}", uri);
                    }
                    return null;
                });
            } catch (ExecutionException e) {
                // won't goto this branch
                assert false;
                return null;
            } catch (RetryException e) {
                Attempt<?> attempt = e.getLastFailedAttempt();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Create activemq connection fail, uri: {}, retry count {} has exhausted",
                            uri, attempt.getAttemptNumber(), attempt.getExceptionCause());
                }
                throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
        return activeMQCache.get(name);
    }
}
