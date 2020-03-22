/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.shallowinggg.doran.common.util;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Static utility methods pertaining to {@code Predicate} instances.
 *
 * <p>All methods return serializable predicates as long as they're given serializable parameters.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/FunctionalExplained">the use of {@code Predicate}</a>.
 *
 * @author Kevin Bourrillion
 */
public class Predicates {

    /**
     * Returns a predicate that always evaluates to {@code true}.
     */
    public static <T> Predicate<T> alwaysTrue() {
        return ObjectPredicate.ALWAYS_TRUE.withNarrowedType();
    }

    /**
     * Returns a predicate that always evaluates to {@code false}.
     */
    public static <T> Predicate<T> alwaysFalse() {
        return ObjectPredicate.ALWAYS_FALSE.withNarrowedType();
    }

    /**
     * Returns a predicate that evaluates to {@code true} if the object reference being tested is
     * null.
     */
    public static <T> Predicate<T> isNull() {
        return ObjectPredicate.IS_NULL.withNarrowedType();
    }

    /**
     * Returns a predicate that evaluates to {@code true} if the object reference being tested is not
     * null.
     */
    public static <T> Predicate<T> notNull() {
        return ObjectPredicate.NOT_NULL.withNarrowedType();
    }

    /**
     * Returns a predicate that evaluates to {@code true} if either of its components evaluates to
     * {@code true}. The components are evaluated in order, and evaluation will be "short-circuited"
     * as soon as a true predicate is found.
     */
    public static <T> Predicate<T> or(Predicate<? super T> first, Predicate<? super T> second) {
        return new OrPredicate<>(Predicates.asList(Assert.notNull(first), Assert.notNull(second)));
    }

    enum ObjectPredicate implements Predicate<Object> {
        /**
         * @see Predicates#alwaysTrue()
         */
        ALWAYS_TRUE {
            @Override
            public boolean test(@Nullable Object o) {
                return true;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysTrue()";
            }
        },
        /**
         * @see Predicates#alwaysFalse()
         */
        ALWAYS_FALSE {
            @Override
            public boolean test(@Nullable Object o) {
                return false;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysFalse()";
            }
        },
        /**
         * @see Predicates#isNull()
         */
        IS_NULL {
            @Override
            public boolean test(@Nullable Object o) {
                return o == null;
            }

            @Override
            public String toString() {
                return "Predicates.isNull()";
            }
        },
        /**
         * @see Predicates#notNull()
         */
        NOT_NULL {
            @Override
            public boolean test(@Nullable Object o) {
                return o != null;
            }

            @Override
            public String toString() {
                return "Predicates.notNull()";
            }
        };

        /**
         * safe contravariant cast
         *
         * @param <T> the type of the input to the predicate
         * @return Predicate
         */
        @SuppressWarnings("unchecked")
        <T> Predicate<T> withNarrowedType() {
            return (Predicate<T>) this;
        }
    }

    private static class OrPredicate<T> implements Predicate<T>, Serializable {
        private final List<? extends Predicate<? super T>> components;

        private OrPredicate(List<? extends Predicate<? super T>> components) {
            this.components = components;
        }

        @Override
        public boolean test(@Nullable T t) {
            // Avoid using the Iterator to avoid generating garbage (issue 820).
            // Components is ArrayList.
            for (int i = 0; i < components.size(); i++) {
                if (components.get(i).test(t)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            // add a random number to avoid collisions with AndPredicate
            return components.hashCode() + 0x053c91cf;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof OrPredicate) {
                OrPredicate<?> that = (OrPredicate<?>) obj;
                return components.equals(that.components);
            }
            return false;
        }

        @Override
        public String toString() {
            return toStringHelper("or", components);
        }

        private static final long serialVersionUID = 0;
    }

    private static String toStringHelper(String methodName, Iterable<?> components) {
        StringBuilder builder = new StringBuilder("Predicates.").append(methodName).append('(');
        boolean first = true;
        for (Object o : components) {
            if (!first) {
                builder.append(',');
            }
            builder.append(o);
            first = false;
        }
        return builder.append(')').toString();
    }

    private static <T> List<Predicate<? super T>> asList(
            Predicate<? super T> first, Predicate<? super T> second) {
        return Arrays.asList(first, second);
    }
}
