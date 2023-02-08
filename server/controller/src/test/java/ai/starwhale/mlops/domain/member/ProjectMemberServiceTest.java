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

package ai.starwhale.mlops.domain.member;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.mapper.ProjectMemberMapper;
import ai.starwhale.mlops.domain.project.po.ProjectMemberEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectMemberServiceTest {

    private ProjectMemberService service;

    private ProjectMemberMapper projectMemberMapper;

    private UserService userService;

    private ProjectService projectService;

    @BeforeEach
    public void setUp() {
        projectMemberMapper = mock(ProjectMemberMapper.class);
        userService = mock(UserService.class);
        given(userService.loadUserById(same(1L)))
                .willReturn(User.builder().id(1L).name("starwhale").build());
        given(userService.loadUserById(same(2L)))
                .willReturn(User.builder().id(2L).name("guest").build());
        given(userService.findRole(same(1L)))
                .willReturn(Role.builder().roleName(Role.NAME_OWNER).build());
        given(userService.findRole(same(2L)))
                .willReturn(Role.builder().roleName(Role.NAME_MAINTAINER).build());
        projectService = mock(ProjectService.class);
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(projectService.getProjectId(same("p1")))
                .willReturn(1L);
        given(projectService.getProjectId(same("2")))
                .willReturn(2L);
        service = new ProjectMemberService(projectMemberMapper, userService, projectService, new IdConverter());
    }

    @Test
    public void testListProjectMembers() {
        given(projectMemberMapper.listByProject(same(1L)))
                .willReturn(List.of(
                        ProjectMemberEntity.builder()
                                .id(1L)
                                .projectId(1L)
                                .userId(1L)
                                .roleId(1L)
                                .build(),
                        ProjectMemberEntity.builder()
                                .id(2L)
                                .projectId(1L)
                                .userId(2L)
                                .roleId(2L)
                                .build()));
        given(projectMemberMapper.listByProject(same(2L)))
                .willReturn(Collections.emptyList());

        var res = service.listProjectMembers("1");
        assertThat(res, allOf(
                notNullValue(),
                hasSize(2)
        ));
        res = service.listProjectMembers("p1");
        assertThat(res, allOf(
                notNullValue(),
                hasSize(2)
        ));
        res = service.listProjectMembers("2");
        assertThat(res, allOf(
                notNullValue(),
                hasSize(0)
        ));
    }

    @Test
    public void testListProjectMembersOfUser() {
        given(projectMemberMapper.listByUser(same(1L)))
                .willReturn(List.of(ProjectMemberEntity.builder()
                                .id(1L)
                                .projectId(1L)
                                .userId(1L)
                                .roleId(1L)
                                .build(),
                        ProjectMemberEntity.builder()
                                .id(2L)
                                .projectId(1L)
                                .userId(2L)
                                .roleId(2L)
                                .build()));
        var res = service.listProjectMembersOfUser(1L);
        assertThat(res, allOf(
                notNullValue(),
                hasSize(2)
        ));
        res = service.listProjectMembersOfUser(2L);
        assertThat(res, allOf(
                notNullValue(),
                hasSize(0)
        ));
    }

    @Test
    public void testAddProjectMember() {
        given(projectMemberMapper.insert(any())).willReturn(1);
        var res = service.addProjectMember(1L, 1L, 1L);
        assertThat(res, is(true));

        res = service.addProjectMember("1", 1L, 1L);
        assertThat(res, is(true));

        given(projectMemberMapper.insertByRoleName(any(), any(), any())).willReturn(1);
        res = service.addProjectMember(1L, 1L, "Owner");
        assertThat(res, is(true));

    }

    @Test
    public void testModifyProjectMember() {
        given(projectMemberMapper.updateRole(same(1L), any()))
                .willReturn(1);
        var res = service.modifyProjectMember("", 1L, 2L);
        assertThat(res, is(true));

        res = service.modifyProjectMember("", 2L, 2L);
        assertThat(res, is(false));
    }

    @Test
    public void testDeleteProjectMember() {
        given(projectMemberMapper.delete(same(1L))).willReturn(1);
        var res = service.deleteProjectMember("", 1L);
        assertThat(res, is(true));

        res = service.deleteProjectMember("", 2L);
        assertThat(res, is(false));
    }

    @Test
    public void testListSystemMembers() {
        given(projectMemberMapper.listByProject(same(0L)))
                .willReturn(List.of(ProjectMemberEntity.builder().id(1L).build(),
                        ProjectMemberEntity.builder().id(2L).build()));

        var res = service.listSystemMembers();
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2))
        ));
    }

    @Test
    public void testListUserRoles() {
        given(projectMemberMapper.listByUser(same(1L)))
                .willReturn(List.of(
                        ProjectMemberEntity.builder().id(1L).build(),
                        ProjectMemberEntity.builder().id(2L).build()
                ));
        given(projectMemberMapper.findByUserAndProject(same(1L), same(1L)))
                .willReturn(
                        ProjectMemberEntity.builder().id(1L).build()
                );

        var res = service.listProjectMembersOfUser(1L);
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2))
        ));
    }

    @Test
    public void testGetUserRoleInProject() {
        given(projectMemberMapper.findByUserAndProject(same(1L), same(1L)))
                .willReturn(ProjectMemberEntity.builder().roleId(1L).build());

        var res = service.getUserRoleInProject(1L, 1L);
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("roleName", is(Role.NAME_OWNER)))
        ));

        res = service.getUserRoleInProject(2L, 1L);
        assertThat(res, allOf(
                nullValue()
        ));
    }
}
