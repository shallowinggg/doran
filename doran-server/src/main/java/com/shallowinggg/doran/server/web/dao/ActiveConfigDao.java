package com.shallowinggg.doran.server.web.dao;

import com.shallowinggg.doran.server.web.entity.ActiveConfig;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    ActiveConfig selectByName(String name);

    /**
     * Insert a new {@link ActiveConfig}.
     *
     * @param config the config to add
     * @return {@code true} if add success
     */
    boolean insertActiveConfig(ActiveConfig config);

    /**
     * Delete active config by its name.
     *
     * @param name config name
     */
    void deleteActiveConfig(String name);

    /**
     * Update active config.
     *
     * @param config new config
     * @throws JsonSerializeException if serialize fail
     */
    void updateActiveConfig(ActiveConfig config);
}
