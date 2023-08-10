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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.api.protobuf.Project.ProjectMemberVo;
import ai.starwhale.mlops.api.protobuf.User.RoleVo;
import ai.starwhale.mlops.api.protobuf.User.UserVo;
import ai.starwhale.mlops.api.protocol.user.UserCheckPasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleAddRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleDeleteRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleUpdateRequest;
import ai.starwhale.mlops.api.protocol.user.UserUpdatePasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserUpdateStateRequest;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class UserControllerTest {

    private UserController controller;

    private UserService userService;

    private ProjectService projectService;

    private MemberService memberService;

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    public void setUp() {
        jwtTokenUtil = mock(JwtTokenUtil.class);
        projectService = mock(ProjectService.class);
        memberService = mock(MemberService.class);
        userService = mock(UserService.class);
        given(userService.currentUser())
                .willReturn(UserVo.newBuilder().setId("1").setName("current").build());
        given(userService.currentUserDetail())
                .willReturn(User.builder().id(1L).name("current").build());
        given(userService.checkCurrentUserPassword("correct"))
                .willReturn(true);
        given(userService.changePassword(argThat(user -> user.getId() == 1L), same("newPassword")))
                .willReturn(true);

        IdConverter idConvertor = new IdConverter();
        controller = new UserController(userService, projectService, memberService, idConvertor, jwtTokenUtil);
    }

    @Test
    public void testListUser() {
        UserVo u1 = UserVo.newBuilder().setId("1").setName("u1").build();
        UserVo u2 = UserVo.newBuilder().setId("2").setName("u2").build();
        given(userService.listUsers(any(User.class), any()))
                .willReturn(PageInfo.of(List.of(u1, u2)));
        given(userService.listUsers(argThat(user -> user.getName().equals("u1")), any()))
                .willReturn(PageInfo.of(List.of(u1)));

        var resp = controller.listUser("", 1, 10);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("total", is(2L))),
                is(hasProperty("list", is(iterableWithSize(2))))
        ));

        resp = controller.listUser("u1", 1, 10);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("total", is(1L))),
                is(hasProperty("list", is(iterableWithSize(1)))),
                is(hasProperty("list", is(hasItem(hasProperty("name", is("u1"))))))
        ));
    }

    @Test
    public void testCreateUser() {
        given(
                userService.createUser(argThat(user -> user.getName().equals("name")), same("password"),
                        same("salt")))
                .willReturn(1L);

        UserRequest request = new UserRequest();
        request.setUserName("name");
        request.setUserPwd("password");
        request.setSalt("salt");
        var resp = controller.createUser(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is("1"));
    }

    @Test
    public void testGetCurrentUser() {
        var resp = controller.getCurrentUser();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("name", is("current")))
        ));
    }

    @Test
    public void testGetCurrentUserRoles() {
        given(projectService.listProjectMemberOfCurrentUser(same("1")))
                .willReturn(List.of(ProjectMemberVo.newBuilder().setId("1").build()));

        var resp = controller.getCurrentUserRoles("1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(iterableWithSize(1))
        ));
    }

    @Test
    public void testGetUserById() {
        given(userService.findUserById(same(1L)))
                .willReturn(UserVo.newBuilder().setId("1").build());

        var resp = controller.getUserById("1");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("id", is("1")))
        ));
    }

    @Test
    public void testUpdateUserPwd() {
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest();
        request.setNewPwd("newPassword");
        request.setCurrentUserPwd("correct");
        var resp = controller.updateUserPwd("1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setCurrentUserPwd("wrong");
        assertThrows(StarwhaleApiException.class,
                () -> controller.updateUserPwd("1", request));
    }

    @Test
    public void testUpdateUserState() {
        given(userService.updateUserState(argThat(user -> user.getId() == 1L), same(true)))
                .willReturn(true);

        UserUpdateStateRequest request = new UserUpdateStateRequest();
        request.setIsEnabled(true);

        var resp = controller.updateUserState("1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testAddUserSystemRole() {
        given(memberService.addProjectMember(same(0L), same(1L), same(1L)))
                .willReturn(true);

        UserRoleAddRequest request = new UserRoleAddRequest();
        request.setCurrentUserPwd("correct");
        request.setUserId("1");
        request.setRoleId("1");

        var resp = controller.addUserSystemRole(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setCurrentUserPwd("wrong");
        assertThrows(StarwhaleApiException.class,
                () -> controller.addUserSystemRole(request));

        request.setCurrentUserPwd("correct");
        request.setUserId("2");
        assertThrows(StarwhaleApiException.class,
                () -> controller.addUserSystemRole(request));
    }

    @Test
    public void testUpdateUserSystemRole() {
        given(memberService.modifyProjectMember(same(1L), same(1L)))
                .willReturn(true);

        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRoleId("1");
        request.setCurrentUserPwd("correct");
        var resp = controller.updateUserSystemRole("1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> controller.updateUserSystemRole("2", request));

        request.setCurrentUserPwd("wrong");
        assertThrows(StarwhaleApiException.class,
                () -> controller.updateUserSystemRole("1", request));
    }

    @Test
    public void testDeleteUserSystemRole() {
        given(memberService.deleteProjectMember(same(1L)))
                .willReturn(true);

        UserRoleDeleteRequest request = new UserRoleDeleteRequest();
        request.setCurrentUserPwd("correct");
        var resp = controller.deleteUserSystemRole("1", request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setCurrentUserPwd("wrong");
        assertThrows(StarwhaleApiException.class,
                () -> controller.deleteUserSystemRole("1", request));
    }

    @Test
    public void testListRoles() {
        given(userService.listRoles())
                .willReturn(List.of(RoleVo.newBuilder().setId("1").build()));

        var resp = controller.listRoles();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("id", is("1"))))
        ));
    }

    @Test
    public void testListSystemRoles() {
        given(projectService.listProjectMembersInProject(same("0")))
                .willReturn(List.of(ProjectMemberVo.newBuilder().setId("1").build()));

        var resp = controller.listSystemRoles();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("id", is("1"))))
        ));
    }

    @Test
    public void testCheckCurrentUserPassword() {
        UserCheckPasswordRequest request = new UserCheckPasswordRequest();
        request.setCurrentUserPwd("correct");

        var resp = controller.checkCurrentUserPassword(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setCurrentUserPwd("wrong");
        resp = controller.checkCurrentUserPassword(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    @Test
    public void testUpdateCurrentUserPassword() {
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest();
        request.setCurrentUserPwd("correct");
        request.setNewPwd("newPassword");

        var resp = controller.updateCurrentUserPassword(request);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        request.setCurrentUserPwd("wrong");
        assertThrows(StarwhaleApiException.class,
                () -> controller.updateCurrentUserPassword(request));
    }

    @Test
    public void testUserToken() {
        given(userService.loadUserById(same(2L)))
                .willReturn(User.builder().id(2L).name("test").build());
        given(jwtTokenUtil.generateAccessToken(argThat(user -> user.getName().equals("test"))))
                .willReturn("token");

        var resp = controller.userToken(2L);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), is("token"));
    }
}
