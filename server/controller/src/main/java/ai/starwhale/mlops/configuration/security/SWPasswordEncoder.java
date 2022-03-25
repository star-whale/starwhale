/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import ai.starwhale.mlops.common.util.Md5Util;
import org.springframework.security.crypto.password.PasswordEncoder;

public class SWPasswordEncoder implements PasswordEncoder {

    public static PasswordEncoder getEncoder(String salt) {
        return new SWPasswordEncoder(salt);
    }

    private final String salt;
    private SWPasswordEncoder(String salt) {
        this.salt = salt;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return Md5Util.md5(rawPassword.toString(), salt);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }

}
