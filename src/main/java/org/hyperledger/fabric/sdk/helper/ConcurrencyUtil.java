package org.hyperledger.fabric.sdk.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConcurrencyUtil {

    private ConcurrencyUtil() {

    }

    /**
     * Waits if necessary for at most the given time for the computations
     * to complete, and then retrieves its results, if available.
     * Implementation is fail fast. Exception is propagated to the caller
     * if any computation finishes with error.
     *
     * @param futures the list of computations
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @param <T>     the result type returned by the futures
     * @return the computed results
     * @throws InterruptedException if the current thread was interrupted
     *                              while waiting
     * @throws TimeoutException     if the wait timed out
     * @throws ExecutionException   if the computation threw an
     *                              exception
     */
    public static <T> Collection<T> get(Collection<Future<T>> futures, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        List<T> result = new ArrayList<>();
        long timeoutInNanos = unit.toNanos(timeout);
        long start = System.nanoTime();
        for (Future<T> future : futures) {
            result.add(future.get(timeoutInNanos, TimeUnit.NANOSECONDS));
            long now = System.nanoTime();
            timeoutInNanos -= (now - start);
            start = now;
        }
        return result;
    }
}
