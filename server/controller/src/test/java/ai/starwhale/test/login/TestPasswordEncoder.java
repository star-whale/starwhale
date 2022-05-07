/**
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

package ai.starwhale.test.login;

import ai.starwhale.mlops.configuration.security.SWPasswordEncoder;
import ai.starwhale.mlops.domain.user.SaltGenerator;
import cn.hutool.core.lang.Dict;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TestPasswordEncoder {

    @Test
    public void testEncoder() {
        SaltGenerator saltGenerator = new SaltGenerator();
        String salt = saltGenerator.salt();
        PasswordEncoder encoder = SWPasswordEncoder.getEncoder(salt);

        Dict dict = Dict.create();


        //String raw = "abcd1234";
        String raw = "asdf7890";
        String encoded = encoder.encode(raw);

        System.out.println("==== raw ====");
        System.out.println(raw);
        System.out.println("==== salt ====");
        System.out.println(salt);
        System.out.println("==== encoded ====");
        System.out.println(encoded);
        Assertions.assertTrue(SWPasswordEncoder.getEncoder(salt).matches(raw, encoded));
    }

}
