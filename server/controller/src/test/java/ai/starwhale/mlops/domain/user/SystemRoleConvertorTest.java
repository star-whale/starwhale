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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemRoleConvertorTest {
    private SystemRoleConvertor systemRoleConvertor;

    @BeforeEach
    public void setUp() {
        IdConvertor idConvertor = new IdConvertor();
        UserConvertor userConvertor = mock(UserConvertor.class);
        given(userConvertor.convert(any(UserEntity.class))).willReturn(UserVo.empty());
        RoleConvertor roleConvertor = mock(RoleConvertor.class);
        given(roleConvertor.convert(any(RoleEntity.class))).willReturn(RoleVo.empty());
        systemRoleConvertor = new SystemRoleConvertor(idConvertor, roleConvertor, userConvertor);
    }

    @Test
    public void testConvert() {
        var res = systemRoleConvertor.convert(null);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("")),
                hasProperty("user", isA(UserVo.class)),
                hasProperty("role", isA(RoleVo.class))
        ));

        res = systemRoleConvertor.convert(ProjectRoleEntity.builder()
                .id(1L)
                .role(RoleEntity.builder().build())
                .user(UserEntity.builder().build())
                .build());

        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("user", isA(UserVo.class)),
                hasProperty("role", isA(RoleVo.class))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(UnsupportedOperationException.class,
                () -> systemRoleConvertor.revert(SystemRoleVo.builder().build()));
    }
}
