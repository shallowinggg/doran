package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.RetryCountExhaustedException;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import com.shallowinggg.doran.common.util.StringUtils;
import com.shallowinggg.doran.common.util.retry.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ActiveMQConsumer extends AbstractBuiltInConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConsumer.class);
    private final String name;
    private final Session session;
    private MessageConsumer consumer;
    private final Charset UTF_8 = StandardCharsets.UTF_8;

    public ActiveMQConsumer(String name, final ActiveMQConfig config,
                            @Nullable ThreadPoolExecutor executor,
                            @Nullable List<MessageListener> listeners) {
        super(executor, listeners);
        Assert.hasText(name, "'name' must has text");
        Assert.notNull(config, "'config' must not be null");
        this.name = name;

        Connection connection;
        String clientId = config.getClientId();
        if (clientId != null) {
            connection = ConnectionFactoryCache.getInstance().getActiveMQConnection(config, clientId);
        } else {
            connection = ConnectionFactoryCache.getInstance().getActiveMQConnection(config, null);
        }

        Retryer<Session> retryer = RetryerBuilder.<Session>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        Session session;
        try {
            session = retryer.call(() -> {
                Session innerSession = null;
                try {
                    innerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageConsumer consumer;
                    switch (config.getDestinationType()) {
                        case PTP:
                            Queue queue = innerSession.createQueue(config.getDestinationName());
                            consumer = innerSession.createConsumer(queue);
                            break;
                        case TOPIC:
                            Assert.hasText(clientId);
                            Topic topic = innerSession.createTopic(config.getDestinationName());
                            if (StringUtils.hasText(config.getSelector())) {
                                consumer = innerSession.createDurableSubscriber(topic, clientId, config.getSelector(), false);
                            } else {
                                consumer = innerSession.createDurableSubscriber(topic, clientId);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid destination type: " + config.getDestinationType());
                    }

                    if (CollectionUtils.isNotEmpty(getMessageListeners())) {
                        consumer.setMessageListener(msg -> executor().execute(() -> {
                            if (msg instanceof TextMessage) {
                                TextMessage textMessage = (TextMessage) msg;
                                try {
                                    String text = textMessage.getText();
                                    Message message = Message.decode(text.getBytes(UTF_8));
                                    for (MessageListener listener : getMessageListeners()) {
                                        if(listener.accept(message)) {
                                            listener.onMessage(message);
                                        }
                                    }
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("ActiveMQ consumer '{}' consume message {} success",
                                                name, message);
                                    }
                                } catch (JMSException e) {
                                    if (LOGGER.isErrorEnabled()) {
                                        LOGGER.error("Get message text fail, content: {}", textMessage, e);
                                    }
                                }
                            }
                        }));
                    }
                    this.consumer = consumer;
                    return innerSession;
                } catch (JMSException e) {
                    if (innerSession != null) {
                        innerSession.close();
                    }
                    // retry
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // handle RuntimeException
            RuntimeException cause = (RuntimeException) e.getCause();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq consumer '{}' fail", name, cause);
            }
            throw cause;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq session for consumer '{}' fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
        this.session = session;
    }

    @Override
    public Message receive() {
        if (CollectionUtils.isNotEmpty(getMessageListeners())) {
            throw new IllegalStateException("ActiveMQ consumer '" + name + "' is configured as async mode");
        }
        try {
            javax.jms.Message msg = consumer.receiveNoWait();
            return convertMessage(msg);
        } catch (JMSException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("'{}' receive message fail", name, e);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        if (CollectionUtils.isNotEmpty(getMessageListeners())) {
            throw new IllegalStateException("ActiveMQ consumer '" + name + "' is configured as async mode");
        }
        try {
            javax.jms.Message msg = consumer.receive(unit.toMillis(timeout));
            return convertMessage(msg);
        } catch (JMSException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("'{}' receive message fail", name, e);
            }
            throw new RuntimeException(e);
        }
    }

    private Message convertMessage(javax.jms.Message msg) {
        if (msg instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) msg;
            try {
                String text = textMessage.getText();
                return Message.decode(text.getBytes(UTF_8));
            } catch (JMSException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Get message text fail, content: {}", textMessage, e);
                }
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public void close() {
        try {
            consumer.close();
            session.close();
        } catch (JMSException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Close activemq consumer '{}' and related session fail", name);
            }
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close activemq consumer '{}' success", name);
        }
    }
}
