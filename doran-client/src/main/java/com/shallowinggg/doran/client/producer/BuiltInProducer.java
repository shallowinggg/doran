package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.Message;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public interface BuiltInProducer {
    long WAIT_ACK_MILLS = SystemPropertyUtil.getInt("com.shallowinggg.producer.waitAckMills", 3000);

    /**
     * Send normal message.
     * <p>
     * If send fail at the first time due to network fluctuation,
     * this message will be resent several times immediately.
     * <p>
     * If MQ Broker send a nack that represent maybe not receive
     * this message, it will be resent in the future again. Default
     * resend time is 3000ms. You can set this value by
     * {@link #WAIT_ACK_MILLS}.
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
     * {@link #WAIT_ACK_MILLS}.
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
