package org.hyperledger.fabric.sdk.helper;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConcurrencyUtilTest {

    @Test
    public void testGet() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executor.submit(new SleepingTask(300, TimeUnit.MILLISECONDS));
        Future<Void> f2 = executor.submit(new SleepingTask(200, TimeUnit.MILLISECONDS));
        Collection<Void> result = ConcurrencyUtil.get(Arrays.asList(f1, f2), 500, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, result.size());
    }

    @Test(expected = TimeoutException.class)
    public void testGetTimeout() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executor.submit(new SleepingTask(500, TimeUnit.MILLISECONDS));
        Future<Void> f2 = executor.submit(new SleepingTask(200, TimeUnit.MILLISECONDS));
        ConcurrencyUtil.get(Arrays.asList(f1, f2), 200, TimeUnit.MILLISECONDS);
    }

    private static class SleepingTask implements Callable<Void> {

        private long duration;
        private TimeUnit unit;

        public SleepingTask(long duration, TimeUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }

        @Override
        public Void call() throws Exception {
            try {
                Thread.sleep(unit.toMillis(duration));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }
}
