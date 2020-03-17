package com.shallowinggg.doran.common.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Miscellaneous collection utility methods.
 *
 * @author shallowinggg
 */
public abstract class CollectionUtils {

    /**
     * Return {@code true} if the supplied Collection is {@code null} or empty.
     * Otherwise, return {@code false}.
     *
     * @param collection the Collection to check
     * @return whether the given Collection is empty
     */
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    /**
     * Return {@code true} if the supplied array is {@code null} or empty.
     * Otherwise, return {@code false}.
     *
     * @param array the array to check
     * @return whether the given array is empty
     */
    public static boolean isEmpty(@Nullable Object[] array) {
        return (array == null || array.length == 0);
    }
}
