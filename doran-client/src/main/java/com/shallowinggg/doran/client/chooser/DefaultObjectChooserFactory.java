package com.shallowinggg.doran.client.chooser;

import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation which uses simple round-robin to
 * choose next Object.
 *
 * @author shallowinggg
 */
public class DefaultObjectChooserFactory<T> implements ObjectChooserFactory<T> {

    @Override
    public ObjectChooser<T> newChooser(T[] objects) {
        Assert.isTrue(!CollectionUtils.isEmpty(objects), "The given object array must has element");
        if (isPowerOf2(objects.length)) {
            return new PowerTwoObjectChooser<>(objects);
        } else {
            return new GenericObjectChooser<>(objects);
        }
    }

    private static boolean isPowerOf2(int num) {
        return (num & -num) == num;
    }

    private static final class GenericObjectChooser<T> implements ObjectChooser<T> {
        private final T[] objects;
        private final AtomicInteger idx = new AtomicInteger(0);

        GenericObjectChooser(T[] objects) {
            this.objects = objects;
        }

        @Override
        public T next() {
            return objects[Math.abs(idx.getAndIncrement() % objects.length)];
        }
    }

    private static final class PowerTwoObjectChooser<T> implements ObjectChooser<T> {
        private final T[] objects;
        private final int mask;
        private final AtomicInteger idx = new AtomicInteger(0);

        PowerTwoObjectChooser(T[] objects) {
            this.objects = objects;
            this.mask = objects.length - 1;
        }

        @Override
        public T next() {
            return objects[idx.getAndIncrement() & mask];
        }
    }
}
