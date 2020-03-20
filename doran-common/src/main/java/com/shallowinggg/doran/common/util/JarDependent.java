package com.shallowinggg.doran.common.util;

import com.shallowinggg.doran.common.MQType;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shallowinggg
 */
public abstract class JarDependent {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarDependent.class);
    private static final MQType USER_DEFINE_MQ_TYPE = MQType.parse(SystemPropertyUtil.get("com.shallowinggg.mqType"));
    private static final String ACTIVEMQ_CLASS_NAME = "org.apache.activemq.ActiveMQConnectionFactory";
    private static final String RABBITMQ_CLASS_NAME = "com.rabbitmq.client.ConnectionFactory";

    private static MQType mqType;

    public static MQType mqType() {
        if (mqType != null) {
            return mqType;
        }
        if (USER_DEFINE_MQ_TYPE != MQType.UNKNOWN) {
            mqType = USER_DEFINE_MQ_TYPE;
        }
        if (mqType == null) {
            try {
                Class.forName(RABBITMQ_CLASS_NAME);
                mqType = MQType.RabbitMQ;
            } catch (ClassNotFoundException e) {
                //
            }
        }
        if (mqType == null) {
            try {
                Class.forName(ACTIVEMQ_CLASS_NAME);
                mqType = MQType.ActiveMQ;
            } catch (ClassNotFoundException e) {
                //
            }
        }
        if(mqType == null) {
            mqType = MQType.UNKNOWN;
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Use {}", mqType);
        }
        return mqType;
    }
}
