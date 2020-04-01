package com.shallowinggg.doran.server.web.dao;

import com.shallowinggg.doran.server.web.entity.ActiveConfig;

/**
 * @author shallowinggg
 */
public interface ActiveConfigDao {

    /**
     * Select active config by its name.
     *
     * @param name config name
     * @return ActiveConfig
     */
    ActiveConfig selectByName(String name);

    /**
     * Insert a new {@link ActiveConfig} if it isn't exist.
     *
     * @param config the config to add
     * @return {@code true} if add success
     */
    boolean insertActiveConfig(ActiveConfig config);

    /**
     * Delete active config by its name.
     *
     * @param name config name
     * @return {@code true} if delete success
     */
    boolean deleteActiveConfig(String name);

    /**
     * Update active config.
     *
     * @param config new config
     * @return {@code true} if update success
     */
    boolean updateActiveConfig(ActiveConfig config);
}
