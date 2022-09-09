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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVo;
import ai.starwhale.mlops.api.protocol.user.UserRoleVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.configuration.security.SwPasswordEncoder;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectRoleMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.mapper.RoleMapper;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserServiceTest {

    private UserService service;
    private UserMapper userMapper;
    private RoleMapper roleMapper;
    private ProjectMapper projectMapper;
    private ProjectRoleMapper projectRoleMapper;
    private ProjectManager projectManager;

    @BeforeEach
    public void setUp() {
        userMapper = mock(UserMapper.class);
        UserEntity u1 = UserEntity.builder()
                .id(1L)
                .userEnabled(1)
                .userName("user1")
                .userPwd("password1")
                .userPwdSalt("salt1")
                .build();
        given(userMapper.findUserByName(same("user1"))).willReturn(u1);
        given(userMapper.findUser(same(1L))).willReturn(u1);
        given(userMapper.findUserByName(same("current")))
                .willReturn(UserEntity.builder()
                        .id(1L)
                        .userName("current")
                        .userEnabled(1)
                        .userPwd(SwPasswordEncoder
                                .getEncoder("current_salt")
                                .encode("current_password"))
                        .userPwdSalt("current_salt")
                        .build());
        roleMapper = mock(RoleMapper.class);
        projectMapper = mock(ProjectMapper.class);
        projectRoleMapper = mock(ProjectRoleMapper.class);
        projectManager = mock(ProjectManager.class);
        given(projectManager.getProjectId(same("0"))).willReturn(0L);
        given(projectManager.getProjectId(same("1"))).willReturn(1L);
        given(projectManager.getProjectId(same("2"))).willReturn(2L);
        given(projectManager.getProjectId(same("3"))).willReturn(3L);
        UserConvertor userConvertor = mock(UserConvertor.class);
        given(userConvertor.convert(any(UserEntity.class)))
                .willAnswer(invocation -> {
                    UserEntity entity = invocation.getArgument(0);
                    return UserVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getUserName())
                            .isEnabled(entity.getUserEnabled() != null && entity.getUserEnabled() == 1)
                            .build();
                });
        RoleConvertor roleConvertor = mock(RoleConvertor.class);
        given(roleConvertor.convert(any(RoleEntity.class)))
                .willReturn(RoleVo.empty());
        UserRoleConvertor userRoleConvertor = mock(UserRoleConvertor.class);
        given(userRoleConvertor.convert(any(ProjectRoleEntity.class)))
                .willReturn(UserRoleVo.builder().build());
        SystemRoleConvertor systemRoleConvertor = mock(SystemRoleConvertor.class);
        given(systemRoleConvertor.convert(any(ProjectRoleEntity.class)))
                .willReturn(SystemRoleVo.builder().build());
        SaltGenerator saltGenerator = mock(SaltGenerator.class);
        given(saltGenerator.salt()).willReturn("salt");
        service = new UserService(userMapper, roleMapper, projectMapper, projectRoleMapper, projectManager,
                userConvertor, roleConvertor, userRoleConvertor, systemRoleConvertor, saltGenerator);

        User current = User.builder().id(1L).name("current").active(true).build();
        var token = new UsernamePasswordAuthenticationToken(current, null);

        SecurityContext context = new SecurityContextImpl(token);

        SecurityContextHolder.setContext(context);
    }

    @Test
    public void testCurrentUserDetail() {
        var res = service.currentUserDetail();
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(1L)),
                hasProperty("name", is("current"))
        ));
        SecurityContextHolder.clearContext();
        assertThrows(StarwhaleApiException.class,
                () -> service.currentUserDetail());
    }

    @Test
    public void testCurrentUser() {
        var res = service.currentUser();
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("current")),
                hasProperty("isEnabled", is(true))
        ));
    }

    @Test
    public void testLoadUserByUsername() {
        var res = service.loadUserByUsername("user1");
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(1L))),
                is(hasProperty("name", is("user1"))),
                is(hasProperty("password", is("password1"))),
                is(hasProperty("salt", is("salt1"))),
                is(hasProperty("active", is(true)))
        ));

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("wrong"));
    }

    @Test
    public void testCheckCurrentUserPassword() {
        var res = service.checkCurrentUserPassword("current_password");
        assertThat(res, is(true));

        res = service.checkCurrentUserPassword("wrong_password");
        assertThat(res, is(false));
    }

    @Test
    public void testLoadUserById() {
        var res = service.loadUserById(1L);
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("id", is(1L))),
                is(hasProperty("name", is("user1"))),
                is(hasProperty("password", is("password1"))),
                is(hasProperty("salt", is("salt1"))),
                is(hasProperty("active", is(true)))
        ));

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserById(2L));
    }

    @Test
    public void testGetProjectRolesOfUser() {
        RoleEntity owner = RoleEntity.builder().id(1L).roleName("Owner").build();
        RoleEntity guest = RoleEntity.builder().id(3L).roleName("Guest").build();
        given(roleMapper.getRolesOfProject(same(1L), same(1L)))
                .willReturn(List.of(owner));
        given(roleMapper.getRolesOfProject(same(2L), same(2L)))
                .willReturn(List.of(guest));
        given(projectMapper.findProject(same(3L)))
                .willReturn(ProjectEntity.builder().id(3L).privacy(1).build());

        var res = service.getProjectRolesOfUser(User.builder().id(1L).build(), "1");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleName", is("Owner"))))
        ));

        res = service.getProjectRolesOfUser(User.builder().id(2L).build(), "2");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleName", is("Guest"))))
        ));

        res = service.getProjectRolesOfUser(User.builder().id(2L).build(), "3");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleName", is("Guest"))))
        ));
    }

    @Test
    public void findUserById() {
        var res = service.findUserById(1L);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("user1"))
        ));
    }

    @Test
    public void testListUser() {
        UserEntity u1 = UserEntity.builder().id(1L).build();
        UserEntity u2 = UserEntity.builder().id(2L).build();
        UserEntity u3 = UserEntity.builder().id(3L).build();
        given(userMapper.listUsers(any()))
                .willReturn(List.of(u1, u2, u3));
        given(userMapper.listUsers(same("u1")))
                .willReturn(List.of(u1));

        var res = service.listUsers(User.builder().build(), new PageParams(1, 10));
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("total", is(3L))),
                is(hasProperty("list", is(iterableWithSize(3))))
        ));
        System.out.println(res);

        res = service.listUsers(User.builder().name("u1").build(), new PageParams(1, 10));
        assertThat(res, allOf(
                notNullValue(),
                is(hasProperty("total", is(1L))),
                is(hasProperty("list", is(iterableWithSize(1)))),
                is(hasProperty("list", is(hasItem(hasProperty("id", is("1"))))))
        ));
    }

    @Test
    public void testCreateUser() {
        given(userMapper.createUser(any(UserEntity.class)))
                .willAnswer(invocation -> {
                    var entity = (UserEntity) invocation.getArgument(0);
                    entity.setId(1L);
                    return null;
                });

        var res = service.createUser(User.builder().name("test").build(), "password", "salt");
        assertThat(res, is(1L));

        assertThrows(StarwhaleApiException.class,
                () -> service.createUser(User.builder().name("user1").build(), "password", "salt"));
    }

    @Test
    public void testChangePassword() {
        given(userMapper.changePassword(argThat(user -> user.getId() == 1L)))
                .willReturn(1);

        var res = service.changePassword(User.builder().id(1L).build(), "new", "old");
        assertThat(res, is(true));

        res = service.changePassword(User.builder().id(2L).build(), "new");
        assertThat(res, is(false));
    }

    @Test
    public void testUpdateUserState() {
        given(userMapper.enableUser(argThat(user -> user.getId() == 1L)))
                .willReturn(1);

        var res = service.updateUserState(User.builder().id(1L).build(), true);
        assertThat(res, is(true));

        res = service.updateUserState(User.builder().id(2L).build(), true);
        assertThat(res, is(false));
    }

    @Test
    public void testListSystemRoles() {
        given(projectRoleMapper.listSystemRoles())
                .willReturn(List.of(ProjectRoleEntity.builder().id(1L).build(),
                        ProjectRoleEntity.builder().id(2L).build()));

        var res = service.listSystemRoles();
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2))
        ));
    }

    @Test
    public void testListRoles() {
        given(roleMapper.listRoles())
                .willReturn(List.of(RoleEntity.builder().id(1L).build(), RoleEntity.builder().id(2L).build()));

        var res = service.listRoles();
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2))
        ));
    }

    @Test
    public void testListUserRoles() {
        given(projectRoleMapper.listUserRoles(same(1L), isNull()))
                .willReturn(List.of(
                        ProjectRoleEntity.builder().id(1L).build(),
                        ProjectRoleEntity.builder().id(2L).build()
                ));
        given(projectRoleMapper.listUserRoles(same(1L), same(1L)))
                .willReturn(List.of(
                        ProjectRoleEntity.builder().id(1L).build()
                ));

        var res = service.listUserRoles(1L, "1");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1))
        ));

        res = service.listUserRoles(1L, "");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2))
        ));

        res = service.listCurrentUserRoles("1");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1))
        ));

    }
}
