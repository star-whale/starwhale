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

package ai.starwhale.mlops.api.config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.configuration.security.JwtProperties;
import ai.starwhale.mlops.configuration.security.ModelServingTokenValidator;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelServingTokenValidatorTest {
    ModelServingTokenValidator modelServingTokenValidator;
    ModelServingMapper modelServingMapper;
    JwtTokenUtil jwtTokenUtil;
    User user;

    @BeforeEach
    public void setup() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("sw_secret");
        jwtTokenUtil = new JwtTokenUtil(jwtProperties);
        modelServingMapper = mock(ModelServingMapper.class);
        modelServingTokenValidator = new ModelServingTokenValidator(jwtTokenUtil, modelServingMapper);
        user = User.builder().id(1L).name("starwhale").build();
    }

    @Test
    public void testToken() {
        String token = modelServingTokenValidator.getToken(user, 1L).split(" ")[1];
        var parsed = jwtTokenUtil.parseJwt(token);

        // not found
        when(modelServingMapper.find(eq(1L))).thenReturn(null);
        Assertions.assertThrowsExactly(SwValidationException.class,
                () -> modelServingTokenValidator.validClaims(parsed));


        var entity = ModelServingEntity.builder()
                .id(1L)
                .isDeleted(0)
                .build();

        // valid
        when(modelServingMapper.find(eq(1L))).thenReturn(entity);
        modelServingTokenValidator.validClaims(parsed);

        // deleted
        entity.setIsDeleted(1);
        when(modelServingMapper.find(eq(1L))).thenReturn(entity);
        var e = Assertions.assertThrowsExactly(SwValidationException.class,
                () -> modelServingTokenValidator.validClaims(parsed));
        Assertions.assertTrue(e.getMessage().contains("deleted"));
        entity.setIsDeleted(0);
    }
}
