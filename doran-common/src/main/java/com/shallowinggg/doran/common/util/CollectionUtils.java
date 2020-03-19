package com.shallowinggg.doran.common.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

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

    /**
     * Return {@code true} if the supplied Map is {@code null} or empty.
     * Otherwise, return {@code false}.
     * @param map the Map to check
     * @return whether the given Map is empty
     */
    public static boolean isEmpty(@Nullable Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    /**
     * Return {@code true} if the supplied Map is not {@code null} and empty.
     * Otherwise, return {@code false}.
     * @param map the Map to check
     * @return whether the given Map is not empty
     */
    public static boolean isNotEmpty(@Nullable Map<?, ?> map) { return !isEmpty(map);}
}
