package com.shallowinggg.doran.client.common;

import com.shallowinggg.doran.common.MQConfig;
import org.jetbrains.annotations.NotNull;

/**
 * A common bean interface defines setter and getter method
 * for {@link MQConfig}.
 *
 * @author shallowinggg
 */
public interface MqConfigBean {

    /**
     * Setter method for {@link MQConfig}.
     *
     * @param mqConfig MQConfig to be set
     */
    void setMqConfig(@NotNull MQConfig mqConfig);

    /**
     * Getter method for {@link MQConfig}.
     *
     * @return MQConfig
     */
    MQConfig getMqConfig();
}
