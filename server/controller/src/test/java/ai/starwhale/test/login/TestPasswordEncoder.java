/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
