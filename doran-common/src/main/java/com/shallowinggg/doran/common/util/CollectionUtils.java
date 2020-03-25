package com.shallowinggg.doran.common.util;

import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Miscellaneous collection utility methods.
 *
 * @author shallowinggg
 */
public final class CollectionUtils {

    private CollectionUtils() {}

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
     * Return {@code true} if the supplied Collection is not {@code null} and empty.
     * Otherwise, return {@code false}.
     *
     * @param collection the Collection to check
     * @return whether the given Collection is not empty
     */
    public static boolean isNotEmpty(@Nullable Collection<?> collection) {
        return !isEmpty(collection);
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

    /**
     * Returns a new array of the given length with the specified component type.
     *
     * @param type the component type
     * @param length the length of the new array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> type, int length) {
        return (T[]) Array.newInstance(type, length);
    }

    /**
     * Returns a new array that contains the concatenated contents of two arrays.
     *
     * @param first the first array of elements to concatenate
     * @param second the second array of elements to concatenate
     * @param type the component type of the returned array
     */
    public static <T> T[] concat(T[] first, T[] second, Class<T> type) {
        T[] result = newArray(type, first.length + second.length);
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements.
     *
     * <p>Note that even when you do need the ability to add or remove, this method provides only a
     * tiny bit of syntactic sugar for {@code newArrayList(}{@link Arrays#asList asList}{@code
     * (...))}, or for creating an empty list then calling {@link Collections#addAll}. This method is
     * not actually very useful and will likely be deprecated in the future.
     */
    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... elements) {
        Assert.notNull(elements);
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    static int computeArrayListCapacity(int arraySize) {
        checkNonNegative(arraySize, "arraySize");
        return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
    }

    @CanIgnoreReturnValue
    static int checkNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
        }
        return value;
    }

    /**
     * Returns the {@code int} nearest in value to {@code value}.
     *
     * @param value any {@code long} value
     * @return the same value cast to {@code int} if it is in the range of the {@code int} type,
     *     {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if it is too
     *     small
     */
    static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
