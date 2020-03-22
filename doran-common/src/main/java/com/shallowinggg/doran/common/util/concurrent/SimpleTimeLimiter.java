package com.shallowinggg.doran.common.util.concurrent;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.shallowinggg.doran.common.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static com.shallowinggg.doran.common.util.Assert.isTrue;
import static com.shallowinggg.doran.common.util.Assert.notNull;

/**
 * A TimeLimiter that runs method calls in the background using an {@link ExecutorService}. If the
 * time limit expires for a given method call, the thread running the call will be interrupted.
 *
 * @author Kevin Bourrillion
 * @author Jens Nyman
 * @since 1.0
 */
public final class SimpleTimeLimiter implements TimeLimiter {

    private final ExecutorService executor;

    private SimpleTimeLimiter(ExecutorService executor) {
        this.executor = notNull(executor);
    }

    /**
     * Creates a TimeLimiter instance using the given executor service to execute method calls.
     *
     * <p><b>Warning:</b> using a bounded executor may be counterproductive! If the thread pool fills
     * up, any time callers spend waiting for a thread may count toward their time limit, and in this
     * case the call may even time out before the target method is ever invoked.
     *
     * @param executor the ExecutorService that will execute the method calls on the target objects;
     *                 for example, a {@link Executors#newCachedThreadPool()}.
     * @since 22.0
     */
    public static SimpleTimeLimiter create(ExecutorService executor) {
        return new SimpleTimeLimiter(executor);
    }

    @Override
    public <T> T newProxy(
            final T target,
            Class<T> interfaceType,
            final long timeoutDuration,
            final TimeUnit timeoutUnit) {
        notNull(target);
        notNull(interfaceType);
        notNull(timeoutUnit);
        checkPositiveTimeout(timeoutDuration);
        isTrue(interfaceType.isInterface(), "interfaceType must be an interface type");

        final Set<Method> interruptibleMethods = findInterruptibleMethods(interfaceType);

        InvocationHandler handler =
                (obj, method, args) -> {
                    Callable<Object> callable =
                            () -> {
                                try {
                                    return method.invoke(target, args);
                                } catch (InvocationTargetException e) {
                                    throw throwCause(e, false /* combineStackTraces */);
                                }
                            };
                    return callWithTimeout(
                            callable, timeoutDuration, timeoutUnit, interruptibleMethods.contains(method));
                };
        return newProxy(interfaceType, handler);
    }

    private static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
        Object object =
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, handler);
        return interfaceType.cast(object);
    }

    private <T> T callWithTimeout(
            Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit, boolean amInterruptible)
            throws Exception {
        notNull(callable);
        notNull(timeoutUnit);
        checkPositiveTimeout(timeoutDuration);

        Future<T> future = executor.submit(callable);

        try {
            if (amInterruptible) {
                try {
                    return future.get(timeoutDuration, timeoutUnit);
                } catch (InterruptedException e) {
                    future.cancel(true);
                    throw e;
                }
            } else {
                return Uninterruptibles.getUninterruptibly(future, timeoutDuration, timeoutUnit);
            }
        } catch (ExecutionException e) {
            throw throwCause(e, true);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new UncheckedTimeoutException(e);
        }
    }

    @CanIgnoreReturnValue
    @Override
    public <T> T callWithTimeout(Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit)
            throws TimeoutException, InterruptedException, ExecutionException {
        notNull(callable);
        notNull(timeoutUnit);
        checkPositiveTimeout(timeoutDuration);

        Future<T> future = executor.submit(callable);

        try {
            return future.get(timeoutDuration, timeoutUnit);
        } catch (InterruptedException | TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            wrapAndThrowExecutionExceptionOrError(e.getCause());
            throw new AssertionError();
        }
    }

    @CanIgnoreReturnValue
    @Override
    public <T> T callUninterruptiblyWithTimeout(
            Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit)
            throws TimeoutException, ExecutionException {
        notNull(callable);
        notNull(timeoutUnit);
        checkPositiveTimeout(timeoutDuration);

        Future<T> future = executor.submit(callable);

        try {
            return Uninterruptibles.getUninterruptibly(future, timeoutDuration, timeoutUnit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            wrapAndThrowExecutionExceptionOrError(e.getCause());
            throw new AssertionError();
        }
    }

    @Override
    public void runWithTimeout(@NotNull Runnable runnable, long timeoutDuration, @NotNull TimeUnit timeoutUnit)
            throws TimeoutException, InterruptedException {
        notNull(runnable);
        notNull(timeoutUnit);
        checkPositiveTimeout(timeoutDuration);

        Future<?> future = executor.submit(runnable);

        try {
            future.get(timeoutDuration, timeoutUnit);
        } catch (InterruptedException | TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            wrapAndThrowRuntimeExecutionExceptionOrError(e.getCause());
            throw new AssertionError();
        }
    }

    @Override
    public void runUninterruptiblyWithTimeout(@NotNull Runnable runnable, long timeoutDuration,
                                              @NotNull TimeUnit timeoutUnit) throws TimeoutException {
        notNull(runnable);
        notNull(timeoutUnit);
        checkPositiveTimeout(timeoutDuration);

        Future<?> future = executor.submit(runnable);

        try {
            Uninterruptibles.getUninterruptibly(future, timeoutDuration, timeoutUnit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            wrapAndThrowRuntimeExecutionExceptionOrError(e.getCause());
            throw new AssertionError();
        }
    }

    private static Exception throwCause(Exception e, boolean combineStackTraces) throws Exception {
        Throwable cause = e.getCause();
        if (cause == null) {
            throw e;
        }
        if (combineStackTraces) {
            StackTraceElement[] combined =
                    CollectionUtils.concat(cause.getStackTrace(), e.getStackTrace(), StackTraceElement.class);
            cause.setStackTrace(combined);
        }
        if (cause instanceof Exception) {
            throw (Exception) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        // The cause is a weird kind of Throwable, so throw the outer exception.
        throw e;
    }

    private static Set<Method> findInterruptibleMethods(Class<?> interfaceType) {
        Set<Method> set = new HashSet<>();
        for (Method m : interfaceType.getMethods()) {
            if (declaresInterruptedEx(m)) {
                set.add(m);
            }
        }
        return set;
    }

    private static boolean declaresInterruptedEx(Method method) {
        for (Class<?> exType : method.getExceptionTypes()) {
            // debate: == or isAssignableFrom?
            if (exType == InterruptedException.class) {
                return true;
            }
        }
        return false;
    }

    private void wrapAndThrowExecutionExceptionOrError(Throwable cause) throws ExecutionException {
        if (cause instanceof Error) {
            throw new ExecutionError((Error) cause);
        } else if (cause instanceof RuntimeException) {
            throw new UncheckedExecutionException(cause);
        } else {
            throw new ExecutionException(cause);
        }
    }

    private void wrapAndThrowRuntimeExecutionExceptionOrError(Throwable cause) {
        if (cause instanceof Error) {
            throw new ExecutionError((Error) cause);
        } else {
            throw new UncheckedExecutionException(cause);
        }
    }

    private static void checkPositiveTimeout(long timeoutDuration) {
        isTrue(timeoutDuration > 0, "timeout must be positive: " + timeoutDuration);
    }
}

