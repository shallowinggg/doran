package com.shallowinggg.doran.client.consumer;

import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.client.common.RetryCountExhaustedException;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import com.shallowinggg.doran.common.util.StringUtils;
import com.shallowinggg.doran.common.util.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ActiveMQConsumer extends AbstractBuiltInConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConsumer.class);
    private final Session session;
    private MessageConsumer consumer;
    private final Charset UTF_8 = StandardCharsets.UTF_8;

    public ActiveMQConsumer(final ActiveMQConfig config, Set<MessageListener> listeners) {
        super(listeners);
        Assert.notNull(config, "'config' must not be null");
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
        final String name = config.getName();
        Session session;
        try {
            session = retryer.call(() -> {
                try {
                    Session innerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
                        consumer.setMessageListener(msg -> {
                            if (msg instanceof TextMessage) {
                                TextMessage textMessage = (TextMessage) msg;
                                try {
                                    String text = textMessage.getText();
                                    Message message = Message.decode(text.getBytes(UTF_8));
                                    for (MessageListener listener : getMessageListeners()) {
                                        listener.onMessage(message);
                                    }
                                } catch (JMSException e) {
                                    if (LOGGER.isErrorEnabled()) {
                                        LOGGER.error(e.getMessage());
                                    }
                                }
                            }
                        });
                    }
                    this.consumer = consumer;
                    return innerSession;
                } catch (JMSException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Create activemq consumer for config {} fail, retry", name, e);
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // handle RuntimeException
            RuntimeException cause = (RuntimeException) e.getCause();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq consumer fail", cause);
            }
            throw cause;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq consumer for config {} fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }
        this.session = session;
    }

    @Override
    public Message receive() {
        return null;
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }
}
