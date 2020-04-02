package com.shallowinggg.doran.common.util;

import com.shallowinggg.doran.common.exception.ClassInstantiationException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Miscellaneous {@code java.lang.Class} utility methods.
 * Mainly for internal use within the framework.
 * <p>
 * Spring's utility class.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Rod Johnson
 * @author Sebastien Deleuze
 * @since 1.0
 */
public final class ClassUtils {

    private ClassUtils() {
    }

    private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

    static {
        Map<Class<?>, Object> values = new HashMap<>();
        values.put(boolean.class, false);
        values.put(byte.class, (byte) 0);
        values.put(short.class, (short) 0);
        values.put(int.class, 0);
        values.put(long.class, (long) 0);
        DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
    }

    /**
     * Instantiate a class using its default constructor
     * (for regular Java classes, expecting a standard no-arg setup).
     * <p>Note that this method tries to set the constructor accessible
     * if given a non-accessible (that is, non-public) constructor.
     *
     * @param clazz the class to instantiate
     * @return the new instance
     * @throws ClassInstantiationException if the bean cannot be instantiated.
     *                                     The cause may notably indicate a {@link NoSuchMethodException} if no
     *                                     primary/default constructor was found, a {@link NoClassDefFoundError}
     *                                     or other {@link LinkageError} in case of an unresolvable class definition
     *                                     (e.g. due to a missing dependency at runtime), or an exception thrown
     *                                     from the constructor invocation itself.
     * @see Constructor#newInstance
     */
    public static <T> T instantiateClass(Class<T> clazz) throws ClassInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new ClassInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            throw new ClassInstantiationException(clazz, "No default constructor found", ex);
        } catch (LinkageError err) {
            throw new ClassInstantiationException(clazz, "Unresolvable class definition", err);
        }
    }

    /**
     * Convenience method to instantiate a class using the given constructor.
     * <p>Note that this method tries to set the constructor accessible if given a
     * non-accessible (that is, non-public) constructor.
     *
     * @param ctor the constructor to instantiate
     * @param args the constructor arguments to apply
     * @return the new instance
     * @throws ClassInstantiationException if the bean cannot be instantiated
     * @see Constructor#newInstance
     */
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws ClassInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            makeAccessible(ctor);

            Class<?>[] parameterTypes = ctor.getParameterTypes();
            Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
            Object[] argsWithDefaultValues = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    Class<?> parameterType = parameterTypes[i];
                    argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                } else {
                    argsWithDefaultValues[i] = args[i];
                }
            }
            return ctor.newInstance(argsWithDefaultValues);

        } catch (InstantiationException ex) {
            throw new ClassInstantiationException(ctor, "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new ClassInstantiationException(ctor, "Is the constructor accessible?", ex);
        } catch (IllegalArgumentException ex) {
            throw new ClassInstantiationException(ctor, "Illegal arguments for constructor", ex);
        } catch (InvocationTargetException ex) {
            throw new ClassInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
        }
    }

    /**
     * Make the given constructor accessible, explicitly setting it accessible
     * if necessary. The {@code setAccessible(true)} method is only called
     * when actually necessary, to avoid unnecessary conflicts with a JVM
     * SecurityManager (if active).
     *
     * @param ctor the constructor to make accessible
     * @see java.lang.reflect.Constructor#setAccessible
     */
    public static void makeAccessible(Constructor<?> ctor) {
        if ((!Modifier.isPublic(ctor.getModifiers()) ||
                !Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) && !ctor.isAccessible()) {
            ctor.setAccessible(true);
        }
    }

    /**
     * Return a descriptive name for the given object's type: usually simply
     * the class name, but component type class name + "[]" for arrays,
     * and an appended list of implemented interfaces for JDK proxies.
     *
     * @param value the value to introspect
     * @return the qualified name of the class
     */
    @Nullable
    public static String getDescriptiveType(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (Proxy.isProxyClass(clazz)) {
            String prefix = clazz.getName() + " implementing ";
            StringJoiner result = new StringJoiner(",", prefix, "");
            for (Class<?> ifc : clazz.getInterfaces()) {
                result.add(ifc.getName());
            }
            return result.toString();
        } else {
            return clazz.getTypeName();
        }
    }

    /**
     * Return the qualified name of the given class: usually simply
     * the class name, but component type class name + "[]" for arrays.
     *
     * @param clazz the class
     * @return the qualified name of the class
     */
    public static String getQualifiedName(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return clazz.getTypeName();
    }
}
