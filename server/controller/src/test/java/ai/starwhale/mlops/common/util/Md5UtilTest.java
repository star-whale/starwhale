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

import ai.starwhale.mlops.domain.user.SaltGenerator;
import org.junit.jupiter.api.Test;

public class Md5UtilTest {

    @Test
    public void testMd5() {
        assertEquals(
                Md5Util.md5("starwhale", "s_salt1"),
                Md5Util.md5("starwhale", "s_salt1")
        );

        String salt = new SaltGenerator().salt();
        assertEquals(
                Md5Util.md5("mypass", salt),
                Md5Util.md5("mypass", salt)
        );

        assertNotEquals(
                Md5Util.md5("starwhale", "s_salt1"),
                Md5Util.md5("starwhale", "s_salt2")
        );

        assertNotEquals(
                Md5Util.md5("starwhale", "s_salt1"),
                Md5Util.md5("starwhale2", "s_salt1")
        );
    }
}
