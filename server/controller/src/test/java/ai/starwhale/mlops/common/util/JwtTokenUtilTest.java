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
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import ai.starwhale.mlops.configuration.security.JwtProperties;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import io.jsonwebtoken.Claims;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    public void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("sw_secret");
        jwtProperties.setIssuer("starwhale");
        jwtProperties.setExpireMinutes(24 * 60L);

        jwtTokenUtil = new JwtTokenUtil(jwtProperties);
    }

    @Test
    public void testJwt() {
        User starwhale = User.builder().id(1L).name("starwhale").build();
        User guest = User.builder().id(2L).name("guest").build();
        String token1 = jwtTokenUtil.generateAccessToken(starwhale);
        String token2 = jwtTokenUtil.generateAccessToken(guest);
        String expiredToken = jwtTokenUtil.generateAccessToken(starwhale, -60 * 1000L, Map.of());

        assertNotEquals(token1, token2);
        assertNotEquals(token1, expiredToken);
        assertThrowsExactly(SwValidationException.class, () -> jwtTokenUtil.parseJwt(expiredToken));

        assertThrowsExactly(SwValidationException.class, () -> jwtTokenUtil.parseJwt(""));
        assertThrowsExactly(SwValidationException.class, () -> jwtTokenUtil.parseJwt(null));
        assertThrowsExactly(SwValidationException.class, () -> jwtTokenUtil.parseJwt(token1 + "1"));
        assertThrowsExactly(SwValidationException.class, () -> jwtTokenUtil.parseJwt(token2 + "b"));

        Claims claims1 = jwtTokenUtil.parseJwt(token1);
        Claims claims2 = jwtTokenUtil.parseJwt(token2);
        assertEquals("starwhale", jwtTokenUtil.getUsername(claims1));
        assertEquals("1", jwtTokenUtil.getUserId(claims1));
        assertEquals("guest", jwtTokenUtil.getUsername(claims2));
        assertEquals("2", jwtTokenUtil.getUserId(claims2));

    }

    public void testWithClaims(){

    }
}
