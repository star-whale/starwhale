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

import ai.starwhale.mlops.api.protocol.user.ProjectMemberVo;
import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.configuration.security.SwPasswordEncoder;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.member.bo.ProjectMember;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.mapper.RoleMapper;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwAuthException.AuthType;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    public static final String USER_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{3,32}$";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MemberService memberService;
    private final IdConverter idConvertor;
    private final SaltGenerator saltGenerator;

    public UserService(UserMapper userMapper, RoleMapper roleMapper,
            MemberService memberService,
            IdConverter idConvertor, SaltGenerator saltGenerator) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.memberService = memberService;
        this.idConvertor = idConvertor;
        this.saltGenerator = saltGenerator;
    }


    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findByName(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found.", username));
        }
        return new User().fromEntity(userEntity);
    }

    public User loadUserById(Long id) {
        UserEntity userEntity = userMapper.find(id);
        if (userEntity == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found.", id));
        }
        return new User().fromEntity(userEntity);
    }

    public Role findRole(Long id) {
        RoleEntity roleEntity = roleMapper.find(id);
        if (roleEntity == null) {
            throw new SwNotFoundException(ResourceType.USER, String.format("Role %s is not found.", id));
        }
        return Role.builder()
                .id(roleEntity.getId())
                .roleName(roleEntity.getRoleName())
                .roleCode(roleEntity.getRoleCode())
                .build();
    }

    public List<Role> getProjectRolesOfUser(User user, Project project) {
        List<Role> list = new ArrayList<>();
        ProjectMember member = memberService.getUserMemberInProject(project.getId(), user.getId());
        if (member != null) {
            Role role = findRole(member.getRoleId());
            list.add(role);
        }

        if (project.getId() != 0) {
            if (project.getPrivacy() == Privacy.PUBLIC) {
                list.add(Role.builder().roleName("Guest").roleCode("GUEST").build());
            }
        }

        return list;
    }

    public Set<Role> getProjectsRolesOfUser(User user, Set<Project> projects) {
        if (projects.isEmpty()) {
            return Set.of();
        }
        // TODO find for all of these projects?
        Project anyProject = projects.stream().findAny().get();
        Set<Role> projectRolesOfUser = new HashSet<>(this.getProjectRolesOfUser(user, anyProject));
        projects.forEach(pj -> projectRolesOfUser.retainAll(this.getProjectRolesOfUser(user, pj)));
        return projectRolesOfUser;
    }

    public UserVo currentUser() {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findByName(user.getName());
        if (userEntity == null) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB,
                            String.format("Unable to find user by name %s", user.getName())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        UserVo userVo = UserVo.fromEntity(userEntity, idConvertor);
        List<ProjectMember> members = memberService.listProjectMembersOfUser(userEntity.getId());
        Map<String, String> roles = new HashMap<>();
        members.forEach(member -> {
            Role role = findRole(member.getRoleId());
            if (Objects.equals(member.getProjectId(), Project.system().getId())) {
                userVo.setSystemRole(role.getRoleCode());
                return;
            }
            String key = idConvertor.convert(member.getProjectId());
            roles.put(key, role.getRoleCode());
        });
        userVo.setProjectRoles(roles);
        return userVo;
    }

    public User currentUserDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER)
                            .tip("Unable to get current user."), HttpStatus.UNAUTHORIZED);
        }

        return (User) authentication.getPrincipal();
    }

    public UserVo findUserById(Long id) {
        return UserVo.fromEntity(userMapper.find(id), idConvertor);
    }


    public PageInfo<UserVo> listUsers(User user, PageParams pageParams) {
        try (var pager = PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize())) {
            List<UserEntity> userEntities = userMapper.list(user.getName(), null);
            return PageUtil.toPageInfo(userEntities, entity -> UserVo.fromEntity(entity, idConvertor));
        }
    }

    @Transactional
    public Long createUser(User user, String password, String salt) {
        UserEntity userByName = userMapper.findByName(user.getName()); // todo lock this row
        if (null != userByName) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.USER, "user already exists"),
                    HttpStatus.BAD_REQUEST);
        }
        String encodedPwd;
        if (StrUtil.isEmpty(salt)) {
            salt = saltGenerator.salt();
            encodedPwd = SwPasswordEncoder.getEncoder(salt).encode(password);
        } else {
            encodedPwd = password;
        }
        UserEntity userEntity = UserEntity.builder()
                .userName(user.getName())
                .userPwd(encodedPwd)
                .userPwdSalt(salt)
                .userEnabled(1)
                .build();
        userMapper.insert(userEntity);
        memberService.addProjectMember(Project.system().getId(), userEntity.getId(), Role.NAME_MAINTAINER);

        log.info("User has been created. ID={}, NAME={}", userEntity.getId(), userEntity.getUserName());

        return userEntity.getId();
    }

    public Boolean changePassword(User user, String newPassword) {
        return changePassword(user, newPassword, null);
    }

    public Boolean changePassword(User user, String newPassword, String oldPassword) {
        String salt = saltGenerator.salt();
        log.info("User password has been changed. ID={}", user.getId());
        return userMapper.updatePassword(user.getId(),
                SwPasswordEncoder.getEncoder(salt).encode(newPassword),
                salt) > 0;
    }

    public Boolean updateUserState(User user, Boolean isEnabled) {
        log.info("User has been {}.", isEnabled ? "enabled" : "disabled");
        return userMapper.updateEnabled(user.getId(),
                Optional.of(isEnabled).orElse(false) ? 1 : 0) > 0;
    }


    public Boolean checkCurrentUserPassword(String password) {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findByName(user.getName());
        if (userEntity == null) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB,
                            String.format("Unable to find user by name %s", user.getName())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        PasswordEncoder passwordEncoder = SwPasswordEncoder.getEncoder(userEntity.getUserPwdSalt());
        return passwordEncoder.matches(password, userEntity.getUserPwd());
    }


    public List<RoleVo> listRoles() {
        return roleMapper.list()
                .stream()
                .map(role -> RoleVo.fromEntity(role, idConvertor))
                .collect(Collectors.toList());
    }

    public RoleVo findRoleById(Long roleId) {
        return RoleVo.fromEntity(roleMapper.find(roleId), idConvertor);
    }

    public List<ProjectMemberVo> listCurrentUserRoles() {
        throw new UnsupportedOperationException("Please use currentUser() instead.");
    }

    public Long getUserId(String user) {
        if (StrUtil.isEmpty(user)) {
            return null;
        }
        if (idConvertor.isId(user)) {
            return idConvertor.revert(user);
        }

        UserEntity entity = userMapper.findByName(user);
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.USER, "User is not found. " + user);
        }
        return entity.getId();
    }

}
