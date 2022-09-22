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

package ai.starwhale.mlops.storage.s3;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.storage.s3.BotoS3Config;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BotoS3ConfigTest {
    @Test
    public void testEnvStr() {
        var tests = Map.of(
                BotoS3Config.AddressingStyleType.AUTO, "{\"addressing_style\": \"auto\"}",
                BotoS3Config.AddressingStyleType.VIRTUAL, "{\"addressing_style\": \"virtual\"}",
                BotoS3Config.AddressingStyleType.PATH, "{\"addressing_style\": \"path\"}"
        );

        tests.forEach((BotoS3Config.AddressingStyleType t, String expect) -> {
            var botoS3Config = new BotoS3Config(t);
            assertThat(botoS3Config.toEnvStr(), is(expect));
        });
    }
}
