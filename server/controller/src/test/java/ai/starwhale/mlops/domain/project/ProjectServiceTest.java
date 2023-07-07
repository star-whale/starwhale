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

package ai.starwhale.mlops.domain.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.member.bo.ProjectMember;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectVisitedMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.sort.Sort;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

public class ProjectServiceTest {

    private ProjectService service;

    private ProjectMapper projectMapper;

    private ProjectVisitedMapper projectVisitedMapper;

    private ProjectDao projectDao;

    private MemberService memberService;

    @BeforeEach
    public void setUp() {
        ProjectEntity project1 = ProjectEntity.builder()
                .id(1L).projectName("p1").ownerId(1L).isDefault(1).isDeleted(0).privacy(1)
                .projectDescription("project1")
                .build();
        ProjectEntity project2 = ProjectEntity.builder()
                .id(2L).projectName("p2").ownerId(2L).isDefault(0).isDeleted(0).privacy(0)
                .projectDescription("project2")
                .build();

        projectMapper = mock(ProjectMapper.class);
        given(projectMapper.find(same(1L))).willReturn(project1);
        given(projectMapper.find(same(2L))).willReturn(project2);
        given(projectMapper.findByName(same("p1"))).willReturn(List.of(project1));
        given(projectMapper.findByName(same("p2"))).willReturn(List.of(project2));
        given(projectMapper.findByNameForUpdateAndOwner(same("p1"), any())).willReturn(project1);
        given(projectMapper.findByNameForUpdateAndOwner(same("p2"), any())).willReturn(project2);
        given(projectMapper.findExistingByNameAndOwner(same("exist_project"), any()))
                .willReturn(project1);
        given(projectMapper.findExistingByNameAndOwnerName(same("exist_project"), any()))
                .willReturn(project2);
        given(projectMapper.listOfUser(anyString(), any(), any()))
                .willReturn(List.of(project1, project2));
        given(projectMapper.listOfUser(same("p1"), any(), any()))
                .willReturn(List.of(project1));

        projectVisitedMapper = mock(ProjectVisitedMapper.class);

        projectDao = mock(ProjectDao.class);
        given(projectDao.getProjectId(same("1"))).willReturn(1L);
        given(projectDao.getProjectId(same("2"))).willReturn(2L);
        given(projectDao.getProjectId(same("p1"))).willReturn(1L);
        given(projectDao.getProjectId(same("p2"))).willReturn(2L);
        given(projectDao.getProjectId(same("3"))).willReturn(3L);
        given(projectDao.getProject(same("1"))).willReturn(project1);
        given(projectDao.getProject(same("2"))).willReturn(project2);
        given(projectDao.getProject(same("p1"))).willReturn(project1);
        given(projectDao.getProject(same("p2"))).willReturn(project2);

        UserService userService = mock(UserService.class);
        given(userService.currentUserDetail()).willReturn(User.builder()
                .name("starwhale")
                .id(1L)
                .roles(Set.of(Role.builder().roleName("Owner").roleCode("OWNER").build()))
                .build());
        given(userService.getProjectRolesOfUser(any(), any()))
                .willReturn(Collections.emptyList());
        given(userService.findRole(same(1L)))
                .willReturn(Role.builder().id(1L)
                        .roleName(Role.NAME_OWNER)
                        .roleCode(Role.CODE_OWNER)
                        .build());
        given(userService.findRole(same(2L)))
                .willReturn(Role.builder().id(2L)
                        .roleName(Role.NAME_MAINTAINER)
                        .roleCode(Role.CODE_MAINTAINER)
                        .build());

        memberService = mock(MemberService.class);

        IdConverter idConvertor = new IdConverter();
        service = new ProjectService(projectMapper,
                projectVisitedMapper,
                projectDao,
                memberService,
                idConvertor,
                userService,
                mock(RuntimeVersionMapper.class),
                mock(ModelVersionMapper.class),
                mock(DatasetVersionMapper.class)
        );
    }

    @Test
    public void testFindProject() {
        var p1 = service.getProjectVo("1");
        assertThat(p1, allOf(
                notNullValue(),
                hasProperty("name", is("p1")),
                hasProperty("id", is("1")),
                hasProperty("privacy", is("PUBLIC")),
                hasProperty("description", is("project1"))
        ));
        var p2 = service.getProjectVo("p2");
        assertThat(p2, allOf(
                notNullValue(),
                hasProperty("name", is("p2")),
                hasProperty("id", is("2")),
                hasProperty("privacy", is("PRIVATE")),
                hasProperty("description", is("project2"))
        ));
    }

    @Test
    public void testListProject() {
        ApplicationContext context = mock(ApplicationContext.class);
        service.setApplicationContext(context);
        Sort sort = mock(Sort.class);
        Mockito.when(context.getBean(any(), same(Sort.class)))
                .thenReturn(sort);
        Mockito.when(sort.list(anyString(), any(), anyBoolean()))
                .thenReturn(List.of(
                        ProjectEntity.builder().id(1L).build(),
                        ProjectEntity.builder().id(2L).build()
                ));

        var res = service.listProject("",
                OrderParams.builder().build(),
                User.builder().build());
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("total", is(2L))
        ));
    }

    @Test
    public void testCreateProject() {
        given(projectMapper.insert(any(ProjectEntity.class)))
                .willAnswer(invocation -> {
                    var entity = (ProjectEntity) invocation.getArgument(0);
                    entity.setId(1L);
                    return null;
                });

        var res = service.createProject(Project.builder()
                .name("test1")
                .owner(User.builder().id(1L).build())
                .privacy(Privacy.PRIVATE)
                .build());
        assertThat(res, is(1L));

        assertThrows(StarwhaleApiException.class,
                () -> service.createProject(Project.builder()
                        .name("exist_project")
                        .owner(User.builder().id(1L).build())
                        .privacy(Privacy.PRIVATE)
                        .build()));
    }

    @Test
    public void testDeleteProject() {
        given(projectMapper.remove(same(1L))).willReturn(1);
        given(projectMapper.remove(same(2L))).willReturn(1);

        var res = service.deleteProject("2");
        assertThat(res, is(true));

        res = service.deleteProject("p2");
        assertThat(res, is(true));

        assertThrows(StarwhaleApiException.class,
                () -> service.deleteProject("1"));

        assertThrows(SwNotFoundException.class,
                () -> service.deleteProject("not_exist"));
    }

    @Test
    public void testRecoverProject() {
        given(projectMapper.find(99L))
                .willReturn(ProjectEntity.builder().id(99L).projectName("del.deleted").build());
        given(projectMapper.listRemovedProjects(anyString(), any()))
                .willReturn(Collections.emptyList());
        given(projectMapper.listRemovedProjects(matches("^one.*"), any()))
                .willReturn(List.of(ProjectEntity.builder().id(1L).build()));
        given(projectMapper.listRemovedProjects(matches("^many.*"), any()))
                .willReturn(List.of(ProjectEntity.builder().id(1L).build(), ProjectEntity.builder().id(2L).build()));
        given(projectMapper.listRemovedProjects(matches("^exist_project.*"), any()))
                .willReturn(List.of(ProjectEntity.builder().id(1L).build()));

        var res = service.recoverProject("99");
        assertThat(res, is(99L));

        res = service.recoverProject("one");
        assertThat(res, is(1L));

        assertThrows(StarwhaleApiException.class,
                () -> service.recoverProject("many"));

        assertThrows(StarwhaleApiException.class,
                () -> service.recoverProject("p1"));

        assertThrows(StarwhaleApiException.class,
                () -> service.recoverProject("exist_project"));
    }

    @Test
    public void testModifyProject() {
        given(projectMapper.update(argThat(p -> p.getId() == 1L)))
                .willReturn(1);
        given(projectMapper.findByNameForUpdateAndOwner(same("p2"), any()))
                .willReturn(ProjectEntity.builder().id(2L).projectName("p2").build());
        var res = service.updateProject("1", "pro1", null, 1L, "PUBLIC");
        assertThat(res, is(true));

        res = service.updateProject("p1", "pro1", null, 1L, "PUBLIC");
        assertThat(res, is(true));

        res = service.updateProject("2", "pro1", null, 1L, "PUBLIC");
        assertThat(res, is(false));

        res = service.updateProject("1", "pro1", null, 1L, "PUBLIC");
        assertThat(res, is(true));

        res = service.updateProject("2", "p2", null, 1L, "PUBLIC");
        assertThat(res, is(false));

        assertThrows(StarwhaleApiException.class,
                () -> service.updateProject("1", "p2", "", 1L, "PUBLIC"));
    }

    @Test
    public void testListProjectMemberOfCurrentUser() {
        given(memberService.getUserMemberInProject(same(1L), same(1L)))
                .willReturn(ProjectMember.builder().id(1L).projectId(1L).userId(1L).roleId(1L).build());
        given(memberService.listProjectMembersOfUser(same(1L)))
                .willReturn(List.of(
                        ProjectMember.builder().id(1L).projectId(1L).userId(1L).roleId(1L).build(),
                        ProjectMember.builder().id(2L).projectId(2L).userId(1L).roleId(2L).build()
                ));

        var res = service.listProjectMemberOfCurrentUser("1");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                hasItem(hasProperty("role", hasProperty("code", is(Role.CODE_OWNER))))
        ));

        res = service.listProjectMemberOfCurrentUser("");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2)),
                hasItem(hasProperty("role", hasProperty("code", is(Role.CODE_OWNER)))),
                hasItem(hasProperty("role", hasProperty("code", is(Role.CODE_MAINTAINER))))
        ));
    }
}
