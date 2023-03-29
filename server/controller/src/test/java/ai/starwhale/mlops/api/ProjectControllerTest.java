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

package ai.starwhale.mlops.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.api.protocol.project.CreateProjectRequest;
import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.project.UpdateProjectRequest;
import ai.starwhale.mlops.api.protocol.user.ProjectMemberVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ProjectControllerTest {

    private ProjectController controller;
    private ProjectService projectService;
    private MemberService memberService;

    @BeforeEach
    public void setUp() {
        projectService = mock(ProjectService.class);
        memberService = mock(MemberService.class);
        UserService userService = mock(UserService.class);
        given(userService.currentUserDetail()).willReturn(User.builder()
                .name("starwhale")
                .id(1L)
                .idTableKey(1L)
                .roles(Set.of(Role.builder().roleName("Owner").roleCode("OWNER").build()))
                .build());
        IdConverter idConvertor = new IdConverter();
        controller = new ProjectController(projectService, userService, memberService, idConvertor);
    }

    @Test
    public void testListProject() {
        given(projectService.listProject(anyString(), any(OrderParams.class), any(User.class)))
                .willReturn(new PageInfo<>(List.of(
                        ProjectVo.builder().name("test1").id("1").build(),
                        ProjectVo.builder().name("test2").id("2").build(),
                        ProjectVo.builder().name("test3").id("3").build()
                )));
        given(projectService.listProject(same("test1"), any(OrderParams.class), any(User.class)))
                .willReturn(new PageInfo<>(List.of(
                        ProjectVo.builder().id("1").build()
                )));
        given(projectService.listProject(same("test2"), any(OrderParams.class), any(User.class)))
                .willReturn(new PageInfo<>(List.of(
                        ProjectVo.builder().id("2").build()
                )));

        var resp = controller.listProject(
                "", 1, 10, "");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
                notNullValue(),
                is(hasSize(3))
        ));

        resp = controller.listProject(
                "test1", 1, 10, "");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("id", is("1"))))
        ));

        resp = controller.listProject(
                "test2", 1, 10, "");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("id", is("2"))))
        ));

        resp = controller.listProject(
                "", 2, 2, "");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
                notNullValue(),
                is(iterableWithSize(3)),
                is(hasItem(hasProperty("id", is("3"))))
        ));

    }

    @Test
    public void testCreateProject() {
        given(projectService.createProject(any(Project.class)))
                .willReturn(1L);
        given(projectService.createProject(argThat(p -> p.getName() == null)))
                .willThrow(StarwhaleApiException.class);

        CreateProjectRequest request = new CreateProjectRequest();
        request.setOwnerId("1");
        request.setProjectName("project1");
        request.setPrivacy("PUBLIC");
        var resp = controller.createProject(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is("1"));

        request.setProjectName(null);
        assertThrows(StarwhaleApiException.class,
                () -> controller.createProject(request));
    }

    @Test
    public void testDeleteProject() {
        given(projectService.deleteProject(anyString()))
                .willReturn(true);
        given(projectService.deleteProject(same("")))
                .willReturn(false);
        given(projectService.deleteProject(isNull()))
                .willThrow(StarwhaleApiException.class);
        var resp = controller.deleteProjectByUrl("project1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteProjectByUrl(""));

        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteProjectByUrl(null));
    }

    @Test
    public void testRecoverProject() {
        given(projectService.recoverProject(same("1")))
                .willReturn(1L);
        given(projectService.recoverProject(same("")))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.recoverProject("1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.recoverProject(""));
    }

    @Test
    public void testGetProject() {
        String name = "project1";
        given(projectService.getProjectVo(same(name)))
                .willReturn(ProjectVo.builder().name(name).build());
        given(projectService.getProjectVo(same("")))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.getProjectByUrl(name);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("name", is(name)))
        ));

        assertThrows(StarwhaleApiException.class,
                () -> controller.getProjectByUrl(""));
    }

    @Test
    public void testUpdateProject() {
        given(projectService.modifyProject(anyString(), any(), any(), any(), any()))
                .willReturn(true);
        String errUrl = "err_url";
        given(projectService.modifyProject(same(errUrl), any(), any(), any(), any()))
                .willReturn(false);
        String errName = "err_name";
        given(projectService.modifyProject(anyString(), same(errName), any(), any(), any()))
                .willThrow(StarwhaleApiException.class);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setProjectName("pro");
        var resp = controller.updateProject("project1",
                request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setProjectName(errName);
        assertThrows(StarwhaleApiException.class,
                () -> controller.updateProject("project1", request));

        assertThrows(StarwhaleApiException.class,
                () -> controller.updateProject(errUrl, request));

    }

    @Test
    public void testListProjectRole() {
        given(projectService.listProjectMembersInProject(same("p1")))
                .willReturn(List.of(ProjectMemberVo.builder().id("1").build()));
        given(projectService.listProjectMembersInProject(same("p2")))
                .willReturn(List.of(ProjectMemberVo.builder().id("2").build()));
        given(projectService.listProjectMembersInProject(isNull()))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.listProjectRole("p1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasItem(hasProperty("id", is("1"))))
        ));

        resp = controller.listProjectRole("p2");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasItem(hasProperty("id", is("2"))))
        ));

        assertThrows(StarwhaleApiException.class,
                () -> controller.listProjectRole(null));

    }

    @Test
    public void testAddProjectRole() {
        given(projectService.addProjectMember(anyString(), anyLong(), anyLong()))
                .willReturn(true);
        String errUrl = "err_url";
        given(projectService.addProjectMember(same(errUrl), anyLong(), anyLong()))
                .willThrow(StarwhaleApiException.class);

        var resp = controller.addProjectRole("p1", "1", "1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.addProjectRole(errUrl, "1", "1"));
    }

    @Test
    public void testDeleteProjectRole() {
        given(memberService.deleteProjectMember(anyLong()))
                .willReturn(true);

        var resp = controller.deleteProjectRole("p1", "1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testModifyProjectRole() {
        given(memberService.modifyProjectMember(anyLong(), anyLong()))
                .willReturn(true);

        var resp = controller.modifyProjectRole("p1", "1", "2");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }
}
