package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.RetryCountExhaustedException;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.retry.*;
import org.apache.activemq.ScheduledMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ActiveMQProducer extends AbstractBuiltInProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQProducer.class);
    private final Session session;
    private MessageProducer producer;
    private final Retryer<Void> messageRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();
    private final Charset UTF_8 = StandardCharsets.UTF_8;

    public ActiveMQProducer(final ActiveMQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        Connection connection = ConnectionFactoryCache.getInstance().getActiveMQConnection(config, null);

        Retryer<Session> retryer = RetryerBuilder.<Session>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        final String name = config.getName();
        Session session;
        try {
            session = retryer.call(() -> {
                try {
                    Session innerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Destination destination;
                    switch (config.getDestinationType()) {
                        case PTP:
                            destination = innerSession.createQueue(config.getDestinationName());
                            break;
                        case TOPIC:
                            destination = innerSession.createTopic(config.getDestinationName());
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid destination type: " + config.getDestinationType());
                    }
                    MessageProducer producer = innerSession.createProducer(destination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    this.producer = producer;
                    return innerSession;
                } catch (JMSException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Create activemq producer for config {} fail, retry", name, e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // handle RuntimeException
            RuntimeException cause = (RuntimeException) e.getCause();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq producer fail", cause);
            }
            throw cause;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq producer for config {} fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }

        this.session = session;
    }

    @Override
    public void sendMessage(Message message) {
        if (executor() == null) {
            throw new IllegalStateException("Producer has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message);
        } else {
            executor().submit(() -> sendMessageInner(message));
        }
    }

    private void sendMessageInner(Message message) {
        try {
            messageRetryer.call(() -> {
                try {
                    String text = new String(message.encode(), UTF_8);
                    TextMessage msg = session.createTextMessage(text);
                    producer.send(msg);
                    return null;
                } catch (JMSException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Send message fail, retry", e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Send message fail, content: {}, retry count {} has exhausted",
                        message, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    @Override
    public void sendMessage(Message message, long delay, TimeUnit unit) {
        if (executor() == null) {
            throw new IllegalStateException("Producer has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message, delay, unit);
        } else {
            executor().submit(() -> sendMessageInner(message, delay, unit));
        }
    }

    private void sendMessageInner(Message message, long delay, TimeUnit unit) {
        try {
            final long time = unit.toMillis(delay);
            messageRetryer.call(() -> {
                try {
                    String text = new String(message.encode(), UTF_8);
                    TextMessage msg = session.createTextMessage(text);
                    msg.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, time);
                    producer.send(msg);
                    return null;
                } catch (JMSException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Send message fail, retry", e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // won't goto this branch
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Send message fail, content: {}, retry count {} has exhausted",
                        message, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
    }

    @Override
    public void startResendTask() {
        // persistence message
    }

    @Override
    public void close() {
        try {
            producer.close();
            session.close();
        } catch (JMSException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Close activemq producer fail");
            }
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close activemq producer success");
        }
    }
}
