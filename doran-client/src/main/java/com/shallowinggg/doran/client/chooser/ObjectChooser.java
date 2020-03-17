package com.shallowinggg.doran.client.chooser;

/**
 * @author shallowinggg
 */
public interface ObjectChooser<T> {

    /**
     * Choose the next object to use.
     *
     * @return next object to use
     */
    T next();
}
