package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.common.Message;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * The basic interface for producer. Every MQ producer
 * should implement this interface.
 * <p>
 * It only supports send normal messages and delay
 * messages currently. Other features maybe add in the
 * future.
 * Besides, the implementations of
 * this interface should provide enough fault-tolerance
 * for sending messages. When remote brokers send ack
 * that represents message is sent success, this message
 * can be removed from unconfirmed collection.
 * If the message is not ack by remote brokers too long,
 * default this time is {@link #WAIT_ACK_MILLIS}, it should
 * be resent. You can specify this time by system property
 * "com.shallowinggg.producer.messageWaitAckMillis". This
 * may lead to more memory cost, but worth it.
 * If message can't be sent too long, it will be removed
 * from resend cache and only be logged. Default invalid
 * is {@link #WAIT_ACK_MILLIS} * 3, see
 * {@link #INVALID_MILLIS} for more information.
 * <p>
 * Every {@link BuiltInProducer} instance should be bound to
 * an {@link EventExecutor}, and an {@link EventExecutor}
 * can be bound with multiple {@link BuiltInProducer}. Every
 * operation in the interface should be executed in the executor
 * that it binds to. This can help avoid concurrency problems.
 *
 * @author shallowinggg
 * @see RabbitMQProducer
 * @see ActiveMQProducer
 */
public interface BuiltInProducer {
    long WAIT_ACK_MILLIS = SystemPropertyUtil.getLong("com.shallowinggg.producer.messageWaitAckMillis", 5000);
    long INVALID_MILLIS = SystemPropertyUtil.getLong("com.shallowinggg.producer.messageInvalidMillis", WAIT_ACK_MILLIS * 3);

    /**
     * Send normal message.
     * <p>
     * If send fail at the first time due to network fluctuation,
     * this message will be resent several times immediately until
     * send success.
     * <p>
     * If MQ Broker send a nack what represents maybe not receive
     * this message, it will be resent in the future again. Default
     * resend time is 3000ms. You can set this value by
     * {@link #WAIT_ACK_MILLIS}.
     *
     * @param message the message to send
     */
    void sendMessage(Message message);

    /**
     * Send delay message.
     * <p>
     * If send fail at the first time due to network fluctuation,
     * this message will be resent several times immediately.
     * <p>
     * If MQ Broker send a nack that represent maybe not receive
     * this message, it will be resent in the future again, and
     * its delay will be calculated with current time. Default
     * resend time is 3000ms. You can set this value by
     * {@link #WAIT_ACK_MILLIS}.
     *
     * @param message the message to send
     * @param delay   the delay of message
     * @param unit    the time unit of delay
     */
    void sendMessage(Message message, long delay, TimeUnit unit);

    /**
     * Register this producer to an {@link EventExecutor}.
     *
     * @param executor the executor to register
     */
    void register(@NotNull EventExecutor executor);

    /**
     * Return the {@link EventExecutor} that this producer binding to.
     *
     * @return EventExecutor
     */
    EventExecutor executor();

    /**
     * Start resend unconfirmed messages task. This task is a
     * scheduled task, and it should be executed in {@link #executor()}.
     */
    void startResendTask();

    /**
     * Close this producer and release all resources it used.
     */
    void close();
}
