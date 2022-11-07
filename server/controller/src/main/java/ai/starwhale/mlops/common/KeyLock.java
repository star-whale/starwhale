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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class KeyLock<T> implements Lock {
    private static final ConcurrentHashMap<Object, LockAndCounter> locksMap = new ConcurrentHashMap<>();

    private final T key;

    public KeyLock(T lockKey) {
        this.key = lockKey;
    }

    private static class LockAndCounter {
        private final Lock lock = new ReentrantLock();
        private final AtomicInteger counter = new AtomicInteger(0);
    }

    private LockAndCounter getLock() {
        return locksMap.compute(key, (key, lockAndCounterInner) -> {
            if (lockAndCounterInner == null) {
                lockAndCounterInner = new LockAndCounter();
            }
            lockAndCounterInner.counter.incrementAndGet();
            return lockAndCounterInner;
        });
    }

    private void cleanup(LockAndCounter lockAndCounterOuter) {
        if (lockAndCounterOuter.counter.decrementAndGet() == 0) {
            locksMap.compute(key, (key, lockAndCounterInner) -> {
                if (lockAndCounterInner == null || lockAndCounterInner.counter.get() == 0) {
                    return null;
                }
                return lockAndCounterInner;
            });
        }
    }

    @Override
    public void lock() {
        LockAndCounter lockAndCounter = getLock();

        lockAndCounter.lock.lock();
    }

    @Override
    public void unlock() {
        LockAndCounter lockAndCounter = locksMap.get(key);
        lockAndCounter.lock.unlock();

        cleanup(lockAndCounter);
    }


    @Override
    public void lockInterruptibly() throws InterruptedException {
        LockAndCounter lockAndCounter = getLock();

        try {
            lockAndCounter.lock.lockInterruptibly();
        } catch (InterruptedException e) {
            cleanup(lockAndCounter);
            throw e;
        }
    }

    @Override
    public boolean tryLock() {
        LockAndCounter lockAndCounter = getLock();

        boolean acquired = lockAndCounter.lock.tryLock();

        if (!acquired) {
            cleanup(lockAndCounter);
        }

        return acquired;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        LockAndCounter lockAndCounter = getLock();

        boolean acquired;
        try {
            acquired = lockAndCounter.lock.tryLock(time, unit);
        } catch (InterruptedException e) {
            cleanup(lockAndCounter);
            throw e;
        }

        if (!acquired) {
            cleanup(lockAndCounter);
        }

        return acquired;
    }

    @Override
    public Condition newCondition() {
        LockAndCounter lockAndCounter = locksMap.get(key);

        return lockAndCounter.lock.newCondition();
    }
}
