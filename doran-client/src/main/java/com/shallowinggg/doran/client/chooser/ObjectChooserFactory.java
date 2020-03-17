package com.shallowinggg.doran.client.chooser;

/**
 * Factory that create new {@link ObjectChooser}s.
 *
 * Implementation of this interface can determine use
 * which strategy that chooses Object by the given
 * object array.
 *
 * @author shallowinggg
 */
public interface ObjectChooserFactory<T> {

    /**
     * Return a new {@link ObjectChooser}.
     *
     * @param objects Objects to be choose
     * @return ObjectChooser Implementation
     */
    ObjectChooser<T> newChooser(T[] objects);
}
