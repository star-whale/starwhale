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

package ai.starwhale.mlops.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class RandomUtilTest {

    @Test
    public void testRandom() {
        String s1 = RandomUtil.randomHexString(24);
        String s2 = RandomUtil.randomHexString(24);
        String s3 = RandomUtil.randomHexString(20);
        assertEquals(s1.length(), 24);
        assertEquals(s2.length(), 24);
        assertNotEquals(s1, s2);
        assertEquals(s3.length(), 20);
    }
}
