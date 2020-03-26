package com.shallowinggg.doran.client.common;

import com.shallowinggg.doran.common.MQConfig;
import org.jetbrains.annotations.NotNull;

/**
 * @author shallowinggg
 */
public interface NameGenerator {
    String generateName(@NotNull MQConfig config);
}
