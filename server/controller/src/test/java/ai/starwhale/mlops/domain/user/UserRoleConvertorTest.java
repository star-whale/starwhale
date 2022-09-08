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

import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.UserRoleVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.project.ProjectConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserRoleConvertorTest {

    private UserRoleConvertor userRoleConvertor;

    @BeforeEach
    public void setUp() {
        IdConvertor idConvertor = new IdConvertor();
        ProjectConvertor projectConvertor = mock(ProjectConvertor.class);
        given(projectConvertor.convert(any(ProjectEntity.class))).willReturn(ProjectVo.empty());
        RoleConvertor roleConvertor = mock(RoleConvertor.class);
        given(roleConvertor.convert(any(RoleEntity.class))).willReturn(RoleVo.empty());
        userRoleConvertor = new UserRoleConvertor(idConvertor, projectConvertor, roleConvertor);
    }

    @Test
    public void testConvert() {
        var res = userRoleConvertor.convert(null);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("")),
                hasProperty("project", isA(ProjectVo.class)),
                hasProperty("role", isA(RoleVo.class))
        ));

        res = userRoleConvertor.convert(ProjectRoleEntity.builder()
                .id(1L)
                .role(RoleEntity.builder().build())
                .project(ProjectEntity.builder().build())
                .build());

        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("project", isA(ProjectVo.class)),
                hasProperty("role", isA(RoleVo.class))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(UnsupportedOperationException.class,
                () -> userRoleConvertor.revert(UserRoleVo.builder().build()));
    }
}
