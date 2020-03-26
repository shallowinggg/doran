package com.shallowinggg.doran.client.producer;

import com.shallowinggg.doran.client.common.ConnectionFactoryCache;
import com.shallowinggg.doran.client.common.Message;
import com.shallowinggg.doran.common.ActiveMQConfig;
import com.shallowinggg.doran.common.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import java.util.concurrent.TimeUnit;

/**
 * @author shallowinggg
 */
public class ActiveMQProducer extends AbstractBuiltInProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQProducer.class);

    public ActiveMQProducer(final ActiveMQConfig config) {
        Assert.notNull(config, "'config' must not be null");
        Connection connection = ConnectionFactoryCache.getInstance().getActiveMQConnection(config, null);

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

    }

    @Override
    public void startResendTask() {

    }

    @Override
    public void close() {

    }
}
