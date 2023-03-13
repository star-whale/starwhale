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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ControllerLockTest {

    private ControllerLockImpl lock;

    @BeforeEach
    public void setUIp() {
        lock = new ControllerLockImpl();
    }

    @Test
    public void testLock() {
        lock.lock("type1", "op1");
        lock.lock("type2", "op2");
        Assertions.assertTrue(lock.isLocked("type1"));
        Assertions.assertTrue(lock.isLocked("type2"));

        lock.unlock("type1", "op1");
        lock.unlock("type2", "opp");
        Assertions.assertFalse(lock.isLocked("type1"));
        Assertions.assertTrue(lock.isLocked("type2"));

        lock.lock("type2", "op3");
        lock.lock("type2", "op4");
        Assertions.assertTrue(lock.isLocked("type2"));

        lock.unlock("type2", "op2");
        Assertions.assertTrue(lock.isLocked("type2"));

        lock.unlock("type2", "op3");
        Assertions.assertTrue(lock.isLocked("type2"));

        lock.unlock("type2", "op4");
        Assertions.assertFalse(lock.isLocked("type2"));

        lock.lock("type1", "op1");
        lock.lock("type2", "op2");
        Assertions.assertTrue(lock.isLocked("type1"));
        Assertions.assertTrue(lock.isLocked("type2"));

        lock.clear();
        Assertions.assertFalse(lock.isLocked("type1"));
        Assertions.assertFalse(lock.isLocked("type2"));
    }

}
