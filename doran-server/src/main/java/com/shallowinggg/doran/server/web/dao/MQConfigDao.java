package com.shallowinggg.doran.server.web.dao;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import org.jetbrains.annotations.Nullable;

/**
 * @author shallowinggg
 */
public interface MQConfigDao {

    /**
     * Serialize a new mq config with json and add it.
     *
     * @param config the config to add
     * @return {@code false} if it is exist, otherwise return {@code true}
     */
    boolean insertMQConfig(MQConfig config);

    /**
     * Delete a mq config by its name and type.
     *
     * @param mqName the name of config
     * @param mqType the type of config
     */
    void deleteMQConfig(String mqName, MQType mqType);

    /**
     * Update mq config.
     *
     * @param config the new config
     */
    void updateMQConfig(MQConfig config);

    /**
     * Select mq config by its name and type.
     *
     * @param mqName the name of config
     * @param mqType the type of config
     * @return mq config
     */
    @Nullable
    MQConfig selectMQConfig(String mqName, MQType mqType);
}
