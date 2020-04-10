package com.shallowinggg.doran.server.web.service;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.MQType;
import com.shallowinggg.doran.server.web.dao.JsonSerializeException;

/**
 * @author shallowinggg
 */
public interface MQConfigService {

    /**
     * Add a new {@link MQConfig} and persist.
     *
     * @param config the config to add
     * @throws JsonSerializeException if serialize fail
     * @return {@code false} if it is exist, otherwise return {@code true}
     */
    boolean insertMQConfig(MQConfig config);

    /**
     * Delete a mq config by its name and type.
     *
     * @param configName the name of config
     * @param mqType the type of config
     */
    void deleteMQConfig(String configName, MQType mqType);

    /**
     * Update mq config.
     *
     * @param config the new config
     */
    void updateMQConfig(MQConfig config);

    /**
     * Select mq config by its name. If it is
     * non exist, you will get a instance of
     * {@link com.shallowinggg.doran.common.EmptyMQConfig}.
     *
     * @param configName the name of config
     * @return mq config if it is exist
     */
    MQConfig selectMQConfig(String configName);

    void activateMQConfig(String configName, MQType mqType);


}
