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

package ai.starwhale.mlops.domain.user;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class SaltGeneratorTest {

    @Test
    public void testSalt() {
        SaltGenerator generator = new SaltGenerator();
        String salt1 = generator.salt();
        String salt2 = generator.salt();
        MatcherAssert.assertThat(salt1, Matchers.hasLength(24));
        MatcherAssert.assertThat(salt1, Matchers.not(salt2));

    }
}
