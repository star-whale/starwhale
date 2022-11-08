/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.common;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KeyLockTest {
    String key1 = "1";
    String key2 = "2";
    ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    public void tearDown() {
        this.executor.shutdownNow();
    }

    @Test
    public void testUseDifferentKeysLock() throws ExecutionException, InterruptedException, TimeoutException {
        KeyLock<Object> lock = new KeyLock<>(key1);
        lock.lock();
        AtomicBoolean anotherThreadWasExecutedBeforeLock = new AtomicBoolean(false);
        AtomicBoolean anotherThreadWasExecutedAfterLock = new AtomicBoolean(false);
        try {
            Future<?> x = executor.submit(() -> {
                KeyLock<Object> anotherLock = new KeyLock<>(key2);
                anotherThreadWasExecutedBeforeLock.set(true);
                anotherLock.lock();
                try {
                    anotherThreadWasExecutedAfterLock.set(true);
                } finally {
                    anotherLock.unlock();
                }
                return "success";
            });

            Assertions.assertEquals("success", x.get(1000, TimeUnit.MICROSECONDS));
        } finally {
            Assertions.assertTrue(anotherThreadWasExecutedBeforeLock.get());
            Assertions.assertTrue(anotherThreadWasExecutedAfterLock.get());
            lock.unlock();
        }
    }

    @Test
    public void testUseSameKeysLock() {
        KeyLock<Object> lock = new KeyLock<>(key1);
        lock.lock();
        AtomicBoolean anotherThreadWasUnExecutedAfterLock = new AtomicBoolean(false);
        try {
            Future<?> x = executor.submit(() -> {
                KeyLock<Object> anotherLock = new KeyLock<>(key1);
                anotherLock.lock();
                try {
                    anotherThreadWasUnExecutedAfterLock.set(true);
                } finally {
                    anotherLock.unlock();
                }
                return "success";
            });
            Assertions.assertThrows(TimeoutException.class, () -> x.get(1000, TimeUnit.MICROSECONDS));

        } finally {
            Assertions.assertFalse(anotherThreadWasUnExecutedAfterLock.get());
            lock.unlock();
        }
    }

    @Test
    public void testMultiThreadsUseSameKeyLock() throws InterruptedException, ExecutionException {
        class TestCallable implements Callable<Long> {
            final String key;
            final boolean returnStart;

            TestCallable(String key, boolean returnStart) {
                this.key = key;
                this.returnStart = returnStart;
            }

            @Override
            public Long call() throws Exception {
                KeyLock<Object> lock = new KeyLock<>(key);
                lock.lock();

                try {
                    long start = System.currentTimeMillis();
                    // mock execute process
                    Thread.sleep(100);
                    return returnStart ? start : System.currentTimeMillis();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally { // CRUCIAL
                    lock.unlock();
                }
            }
        }

        Future<Long> first = executor.submit(new TestCallable(key1, false));
        Thread.sleep(10);
        Future<Long> second = executor.submit(new TestCallable(key1, true));

        Assertions.assertTrue(second.get() >= first.get());
    }
}
