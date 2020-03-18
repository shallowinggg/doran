package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.MqConfig;
import org.jetbrains.annotations.NotNull;

/**
 * A common bean interface defines setter and getter method
 * for {@link MqConfig}.
 *
 * @author shallowinggg
 */
public interface MqConfigBean {

    /**
     * Setter method for {@link MqConfig}.
     *
     * @param mqConfig MqConfig to be set
     */
    void setMqConfig(@NotNull MqConfig mqConfig);

    /**
     * Getter method for {@link MqConfig}.
     *
     * @return MqConfig
     */
    MqConfig getMqConfig();
}
