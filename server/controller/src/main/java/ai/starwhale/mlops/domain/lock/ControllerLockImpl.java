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

package ai.starwhale.mlops.domain.lock;


import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ControllerLockImpl implements ControllerLock {

    private final HashMultimap<String, String> lock = HashMultimap.create();

    public void lock(String type, String operator) {
        log.info(String.format("Lock -- type=%s, operator=%s", type, operator));
        synchronized (lock) {
            lock.put(type, operator);
        }
    }

    public void unlock(String type, String operator) {
        log.info(String.format("UnLock -- type=%s, operator=%s", type, operator));
        synchronized (lock) {
            lock.remove(type, operator);
        }
    }

    public boolean isLocked(String type) {
        return lock.containsKey(type);
    }

    @Override
    public void clear() {
        synchronized (lock) {
            lock.clear();
        }
    }
}
