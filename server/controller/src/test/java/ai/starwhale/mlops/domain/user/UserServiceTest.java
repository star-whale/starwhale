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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.configuration.security.SwPasswordEncoder;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.member.bo.ProjectMember;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.converter.RoleVoConverter;
import ai.starwhale.mlops.domain.user.converter.UserVoConverter;
import ai.starwhale.mlops.domain.user.mapper.RoleMapper;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import java.util.Set;
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
    private MemberService memberService;

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
        given(userMapper.findByName(same("user1"))).willReturn(u1);
        given(userMapper.find(same(1L))).willReturn(u1);
        given(userMapper.findByName(same("current")))
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
        RoleEntity owner = RoleEntity.builder()
                .id(1L).roleName(Role.NAME_OWNER).roleCode(Role.CODE_OWNER).build();
        RoleEntity maintainer = RoleEntity.builder()
                .id(2L).roleName(Role.NAME_MAINTAINER).roleCode(Role.CODE_MAINTAINER).build();
        RoleEntity guest = RoleEntity.builder()
                .id(3L).roleName(Role.NAME_GUEST).roleCode(Role.CODE_GUEST).build();
        given(roleMapper.find(same(1L))).willReturn(owner);
        given(roleMapper.find(same(2L))).willReturn(maintainer);
        given(roleMapper.find(same(3L))).willReturn(guest);

        memberService = mock(MemberService.class);
        var idConverter = new IdConverter();

        SaltGenerator saltGenerator = mock(SaltGenerator.class);
        given(saltGenerator.salt()).willReturn("salt");
        service = new UserService(
                userMapper,
                roleMapper,
                memberService,
                idConverter,
                saltGenerator,
                new UserVoConverter(idConverter),
                new RoleVoConverter(idConverter)
        );

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
        assertThrows(
                StarwhaleApiException.class,
                () -> service.currentUserDetail()
        );
    }

    @Test
    public void testCurrentUser() {
        given(memberService.listProjectMembersOfUser(same(1L)))
                .willReturn(List.of(
                        ProjectMember.builder()
                                .projectId(0L)
                                .roleId(1L)
                                .build(),
                        ProjectMember.builder()
                                .projectId(1L)
                                .roleId(2L)
                                .build(),
                        ProjectMember.builder()
                                .projectId(2L)
                                .roleId(3L)
                                .build()
                ));
        var res = service.currentUser();
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("current")),
                hasProperty("isEnabled", is(true)),
                hasProperty("systemRole", is(Role.CODE_OWNER)),
                hasProperty("projectRoles", allOf(
                        hasEntry("1", Role.CODE_MAINTAINER),
                        hasEntry("2", Role.CODE_GUEST)
                ))
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

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("wrong")
        );
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

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserById(2L)
        );
    }

    @Test
    public void testGetProjectRolesOfUser() {
        given(memberService.getUserMemberInProject(same(1L), same(1L)))
                .willReturn(ProjectMember.builder().roleId(1L).build());
        given(memberService.getUserMemberInProject(same(2L), same(2L)))
                .willReturn(ProjectMember.builder().roleId(2L).build());

        var res = service.getProjectRolesOfUser(
                User.builder().id(1L).build(),
                Project.builder().id(1L).privacy(Privacy.PUBLIC).build()
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2)),
                is(hasItem(hasProperty("roleName", is(Role.NAME_OWNER)))),
                is(hasItem(hasProperty("roleCode", is(Role.CODE_GUEST))))
        ));

        res = service.getProjectRolesOfUser(
                User.builder().id(2L).build(),
                Project.builder().id(2L).privacy(Privacy.PRIVATE).build()
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleName", is(Role.NAME_MAINTAINER))))
        ));

        res = service.getProjectRolesOfUser(
                User.builder().id(2L).build(),
                Project.builder().id(3L).privacy(Privacy.PUBLIC).build()
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleName", is(Role.NAME_GUEST))))
        ));
    }

    @Test
    public void testGetProjectsRolesOfUser() {
        given(memberService.getUserMemberInProject(same(1L), same(1L)))
                .willReturn(ProjectMember.builder().roleId(1L).build());
        given(memberService.getUserMemberInProject(same(2L), same(2L)))
                .willReturn(ProjectMember.builder().roleId(2L).build());
        given(memberService.getUserMemberInProject(same(3L), same(1L)))
                .willReturn(ProjectMember.builder().roleId(1L).build());

        var res = service.getProjectsRolesOfUser(
                User.builder().id(1L).build(),
                Set.of(Project.builder().id(1L).privacy(Privacy.PUBLIC).build())
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2)),
                is(hasItem(hasProperty("roleCode", is(Role.CODE_OWNER)))),
                is(hasItem(hasProperty("roleCode", is(Role.CODE_GUEST))))
        ));

        res = service.getProjectsRolesOfUser(
                User.builder().id(2L).build(),
                Set.of(
                        Project.builder().id(2L).privacy(Privacy.PUBLIC).build(),
                        Project.builder().id(3L).privacy(Privacy.PRIVATE).build()
                )
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(0))
        ));

        res = service.getProjectsRolesOfUser(
                User.builder().id(2L).build(),
                Set.of(
                        Project.builder().id(2L).privacy(Privacy.PUBLIC).build(),
                        Project.builder().id(3L).privacy(Privacy.PUBLIC).build()
                )
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleCode", is(Role.CODE_GUEST))))
        ));

        res = service.getProjectsRolesOfUser(
                User.builder().id(1L).build(),
                Set.of(
                        Project.builder().id(1L).privacy(Privacy.PRIVATE).build(),
                        Project.builder().id(3L).privacy(Privacy.PRIVATE).build()
                )
        );
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("roleCode", is(Role.CODE_OWNER))))
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
        UserEntity u1 = UserEntity.builder().id(1L).userName("user1").build();
        UserEntity u2 = UserEntity.builder().id(2L).userName("user2").build();
        UserEntity u3 = UserEntity.builder().id(3L).userName("user3").build();
        given(userMapper.list(any(), any()))
                .willReturn(List.of(u1, u2, u3));
        given(userMapper.list(same("u1"), any()))
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
        given(userMapper.insert(any(UserEntity.class)))
                .willAnswer(invocation -> {
                    var entity = (UserEntity) invocation.getArgument(0);
                    entity.setId(1L);
                    return null;
                });

        var res = service.createUser(User.builder().name("test").build(), "password", "salt");
        assertThat(res, is(1L));

        assertThrows(
                StarwhaleApiException.class,
                () -> service.createUser(User.builder().name("user1").build(), "password", "salt")
        );
    }

    @Test
    public void testChangePassword() {
        given(userMapper.updatePassword(same(1L), anyString(), anyString()))
                .willReturn(1);

        var res = service.changePassword(User.builder().id(1L).build(), "new", "old");
        assertThat(res, is(true));

        res = service.changePassword(User.builder().id(2L).build(), "new");
        assertThat(res, is(false));
    }

    @Test
    public void testUpdateUserState() {
        given(userMapper.updateEnabled(same(1L), any()))
                .willReturn(1);

        var res = service.updateUserState(User.builder().id(1L).build(), true);
        assertThat(res, is(true));

        res = service.updateUserState(User.builder().id(2L).build(), true);
        assertThat(res, is(false));
    }


    @Test
    public void testListRoles() {
        given(roleMapper.list())
                .willReturn(List.of(
                        RoleEntity.builder().id(1L).roleName("foo").roleCode("code1").build(),
                        RoleEntity.builder().id(2L).roleName("bar").roleCode("code2").build()
                ));

        var res = service.listRoles();
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2))
        ));
    }


    @Test
    public void testGetUserId() {
        var res = service.getUserId("user1");
        assertThat(res, is(1L));

        res = service.getUserId("2");
        assertThat(res, is(2L));

        res = service.getUserId("");
        assertThat(res, nullValue());

        assertThrows(SwNotFoundException.class, () -> service.getUserId("not_exist"));
    }
}
