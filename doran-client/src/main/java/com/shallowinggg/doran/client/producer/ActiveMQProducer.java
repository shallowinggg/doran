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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ActiveMQProducer extends AbstractBuiltInProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQProducer.class);
    private final String name;
    private final Session session;
    private MessageProducer producer;
    private long seq = 0;
    private final ResendCache resendCache = new ResendCache();
    private final Retryer<Void> messageRetryer = RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();
    private final Charset UTF_8 = StandardCharsets.UTF_8;

    public ActiveMQProducer(String name, ActiveMQConfig config) {
        Assert.hasText(name, "'name' must has text");
        Assert.notNull(config, "'config' must not be null");
        Connection connection = ConnectionFactoryCache.getInstance().getActiveMQConnection(config, null);

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
                    Destination destination;
                    switch (config.getDestinationType()) {
                        case PTP:
                            destination = innerSession.createQueue(config.getDestinationName());
                            break;
                        case TOPIC:
                            destination = innerSession.createTopic(config.getDestinationName());
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid destination type " + config.getDestinationType());
                    }
                    MessageProducer producer = innerSession.createProducer(destination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    this.producer = producer;
                    return innerSession;
                } catch (JMSException e) {
                    if (innerSession != null) {
                        innerSession.close();
                    }
                    throw e;
                }
            });
        } catch (ExecutionException e) {
            // handle RuntimeException
            RuntimeException cause = (RuntimeException) e.getCause();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq producer {} fail", name, cause);
            }
            throw cause;
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Create activemq producer {} fail, retry count {} has exhausted",
                        name, attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
            throw new RetryCountExhaustedException((int) attempt.getAttemptNumber(), attempt.getExceptionCause());
        }

        this.name = name;
        this.session = session;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ActiveMQ producer {} build success, {} name: {}",
                    name, config.getDestinationType(), config.getDestinationName());
        }
    }

    @Override
    public void sendMessage(Message message) {
        if (executor() == null) {
            throw new IllegalStateException("Producer " + name + " has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message);
        } else {
            executor().submit(() -> sendMessageInner(message));
        }
    }

    private void sendMessageInner(Message message) {
        TextMessage msg = null;
        try {
            String text = new String(message.encode(), UTF_8);
            msg = session.createTextMessage(text);

            TextMessage innerMsg = msg;
            messageRetryer.call(() -> {
                producer.send(innerMsg);
                return null;
            });
        } catch (JMSException e) {
            // won't goto this branch
            // TextMessage won't be read-only
            assert false;
        } catch (ExecutionException e) {
            // handle RuntimeException for producer#send(TextMessage)
            throw (RuntimeException) e.getCause();
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Producer {} send message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, getText(msg), attempt.getAttemptNumber(), attempt.getExceptionCause());
            }

            // only resend when occur network problems, for producer is persistent
            long deliveryTag = seq++;
            resendCache.put(deliveryTag, msg);
        }
    }

    @Override
    public void sendMessage(Message message, long delay, TimeUnit unit) {
        if (executor() == null) {
            throw new IllegalStateException("Producer " + name + " has not initialized success, executor is null");
        }

        if (executor().inEventLoop()) {
            sendMessageInner(message, delay, unit);
        } else {
            executor().submit(() -> sendMessageInner(message, delay, unit));
        }
    }

    private void sendMessageInner(Message message, long delay, TimeUnit unit) {
        final long time = unit.toMillis(delay);
        TextMessage msg = null;
        try {
            String text = new String(message.encode(), UTF_8);
            msg = session.createTextMessage(text);
            msg.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, time);

            TextMessage innerMsg = msg;
            messageRetryer.call(() -> {
                producer.send(innerMsg);
                return null;
            });
        } catch (JMSException e) {
            // won't goto this branch
            // TextMessage won't be read-only
            assert false;
        } catch (ExecutionException e) {
            // handle RuntimeException for producer#send(TextMessage)
            throw (RuntimeException) e.getCause();
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Producer {} send message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, getText(msg), attempt.getAttemptNumber(), attempt.getExceptionCause());
            }

            // only resend when occur network problems, for producer is persistent
            long deliveryTag = seq++;
            resendCache.put(deliveryTag, msg, time);
        }
    }

    private boolean reSendMessageInner(TextMessage message) {
        try {
            messageRetryer.call(() -> {
                producer.send(message);
                return null;
            });
            return true;
        } catch (ExecutionException e) {
            // handle RuntimeException for producer#send(TextMessage)
            throw (RuntimeException) e.getCause();
        } catch (RetryException e) {
            Attempt<?> attempt = e.getLastFailedAttempt();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Producer {} send message fail, content: {}, retry count {} has exhausted, retry in the future",
                        name, getText(message), attempt.getAttemptNumber(), attempt.getExceptionCause());
            }
        }
        return false;
    }

    @Override
    public void startResendTask() {
        if (executor() == null) {
            throw new IllegalStateException("Producer " + name + " has not initialized success, executor is null");
        }
        executor().scheduleAtFixedRate(this::resendNackMessages, WAIT_ACK_MILLIS, WAIT_ACK_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        try {
            producer.close();
            session.close();
        } catch (JMSException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Close activemq producer {} and related session fail", name, e);
            }
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close activemq producer {} success", name);
        }
    }

    private void resendNackMessages() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, ResendMessage>> itr = resendCache.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Long, ResendMessage> entry = itr.next();
            ResendMessage message = entry.getValue();
            final TextMessage msg = message.content;

            // valid, resend
            if (message.sendTime + WAIT_ACK_MILLIS > now) {
                if (message.delay == 0) {
                    executor().submit(() -> {
                        if (reSendMessageInner(msg)) {
                            resendCache.delete(entry.getKey());
                        }
                    });
                } else {
                    final long newDelay = Math.max(0, message.sendTime + message.delay - now);
                    executor().submit(() -> {
                        try {
                            msg.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, newDelay);
                        } catch (JMSException e) {
                            // won't goto this branch
                            // TextMessage won't be read-only
                            assert false;
                        }
                        if (reSendMessageInner(msg)) {
                            resendCache.delete(entry.getKey());
                        }
                    });
                }
            } else if (message.sendTime + INVALID_MILLIS > now) {
                // invalid, remove
                itr.remove();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Message {} send fail, remove it from resend cache, invalid millis: {}",
                            getText(msg), INVALID_MILLIS);
                }
            }
        }
    }

    private String getText(TextMessage msg) {
        try {
            return msg.getText();
        } catch (JMSException e) {
            // won't goto this branch
            // msg#text must exist
            assert false;
        }
        return null;
    }

    public static class ResendCache {
        /**
         * Use sorted map to store unconfirmed messages.
         * Whatever single or multiple ack, skip list can
         * guarantee good performance.
         * TODO: implement a normal skip list for single thread env.
         */
        private final SortedMap<Long, ResendMessage> unconfirmedMap = new ConcurrentSkipListMap<>();

        public void put(Long uniqueId, TextMessage message) {
            unconfirmedMap.put(uniqueId, ResendMessage.create(message));
        }

        public void put(Long uniqueId, TextMessage message, long delay) {
            unconfirmedMap.put(uniqueId, ResendMessage.create(message, delay));
        }

        public void delete(long uniqueId) {
            unconfirmedMap.remove(uniqueId);
        }

        public Set<Map.Entry<Long, ResendMessage>> entrySet() {
            return unconfirmedMap.entrySet();
        }
    }

    private static class ResendMessage {
        final TextMessage content;
        final long delay;
        final long sendTime;

        ResendMessage(final TextMessage content, final long delay) {
            this.content = content;
            this.delay = delay;
            this.sendTime = System.currentTimeMillis();
        }

        static ResendMessage create(final TextMessage content) {
            return new ResendMessage(content, 0);
        }

        static ResendMessage create(final TextMessage content, final long delay) {
            return new ResendMessage(content, delay);
        }
    }
}
