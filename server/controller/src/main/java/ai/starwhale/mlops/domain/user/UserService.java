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

import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVo;
import ai.starwhale.mlops.api.protocol.user.UserRoleVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.configuration.security.SwPasswordEncoder;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectRoleMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.mapper.RoleMapper;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwAuthException.AuthType;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Slf4j
@Service
public class UserService implements UserDetailsService {

    public static final String USER_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{3,32}$";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ProjectMapper projectMapper;
    private final ProjectRoleMapper projectRoleMapper;
    private final ProjectManager projectManager;

    private final IdConvertor idConvertor;
    private final UserConvertor userConvertor;
    private final RoleConvertor roleConvertor;
    private final UserRoleConvertor userRoleConvertor;
    private final SystemRoleConvertor systemRoleConvertor;
    private final SaltGenerator saltGenerator;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, ProjectMapper projectMapper,
            ProjectRoleMapper projectRoleMapper, ProjectManager projectManager, IdConvertor idConvertor,
            UserConvertor userConvertor,
            RoleConvertor roleConvertor, UserRoleConvertor userRoleConvertor, SystemRoleConvertor systemRoleConvertor,
            SaltGenerator saltGenerator) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.projectMapper = projectMapper;
        this.projectRoleMapper = projectRoleMapper;
        this.projectManager = projectManager;
        this.idConvertor = idConvertor;
        this.userConvertor = userConvertor;
        this.roleConvertor = roleConvertor;
        this.userRoleConvertor = userRoleConvertor;
        this.systemRoleConvertor = systemRoleConvertor;
        this.saltGenerator = saltGenerator;
    }


    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findUserByName(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found.", username));
        }
        return new User().fromEntity(userEntity);
    }

    public User loadUserById(Long id) {
        UserEntity userEntity = userMapper.findUser(id);
        if (userEntity == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found.", id));
        }
        return new User().fromEntity(userEntity);
    }

    public List<Role> getProjectRolesOfUser(User user, String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);

        List<RoleEntity> projectRolesOfUser = roleMapper.getRolesOfProject(
                user.getId(), projectId);

        List<Role> list = projectRolesOfUser.stream()
                .map(entity -> Role.builder()
                        .roleName(entity.getRoleName())
                        .roleCode(entity.getRoleCode())
                        .build())
                .collect(Collectors.toList());
        if (projectId != 0) {
            ProjectEntity project = projectMapper.findProject(projectId);
            if (project != null && project.getPrivacy() == Privacy.PUBLIC.getValue()) {
                list.add(Role.builder().roleName("Guest").roleCode("GUEST").build());
            }
        }

        return list;
    }

    public Set<Role> getProjectsRolesOfUser(User user, Set<String> projects) {
        if (projects.isEmpty()) {
            return Set.of();
        }
        String anyProject = projects.stream().findAny().get();
        Set<Role> projectRolesOfUser = this.getProjectRolesOfUser(user, anyProject).stream()
                .collect(Collectors.toSet());
        projects.forEach(pj -> {
            projectRolesOfUser.retainAll(this.getProjectRolesOfUser(user, pj));
        });
        return projectRolesOfUser;
    }

    public UserVo currentUser() {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findUserByName(user.getName());
        if (userEntity == null) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB,
                            String.format("Unable to find user by name %s", user.getName())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        UserVo userVo = userConvertor.convert(userEntity);
        List<ProjectRoleEntity> roleEntities = projectRoleMapper.listUserRoles(userEntity.getId(),
                null);
        Map<String, String> roles = new HashMap<>();
        roleEntities.forEach(entity -> {
            if (entity.getProject().getId() == 0) {
                userVo.setSystemRole(entity.getRole().getRoleCode());
                return;
            }
            String key = idConvertor.convert(entity.getProject().getId());
            roles.put(key, entity.getRole().getRoleCode());
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
        return userConvertor.convert(userMapper.findUser(id));
    }


    public PageInfo<UserVo> listUsers(User user, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<UserEntity> userEntities = userMapper.listUsers(user.getName());

        return PageUtil.toPageInfo(userEntities, userConvertor::convert);
    }


    public Long createUser(User user, String password, String salt) {
        UserEntity userByName = userMapper.findUserByName(user.getName()); // todo lock this row
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
        userMapper.createUser(userEntity);
        projectRoleMapper.addProjectRoleByName(userEntity.getId(), 0L, Role.NAME_MAINTAINER);

        log.info("User has been created. ID={}, NAME={}", userEntity.getId(), userEntity.getUserName());

        return userEntity.getId();
    }

    public Boolean changePassword(User user, String newPassword) {
        return changePassword(user, newPassword, null);
    }

    public Boolean changePassword(User user, String newPassword, String oldPassword) {
        String salt = saltGenerator.salt();
        UserEntity userEntity = UserEntity.builder()
                .id(user.getId())
                .userPwd(SwPasswordEncoder.getEncoder(salt).encode(newPassword))
                .userPwdSalt(salt)
                .build();
        log.info("User password has been changed. ID={}", user.getId());
        return userMapper.changePassword(userEntity) > 0;
    }

    public Boolean updateUserState(User user, Boolean isEnabled) {
        UserEntity userEntity = UserEntity.builder()
                .id(user.getId())
                .userEnabled(Optional.of(isEnabled).orElse(false) ? 1 : 0)
                .build();
        log.info("User has been {}.", isEnabled ? "enabled" : "disabled");
        return userMapper.enableUser(userEntity) > 0;
    }


    public Boolean checkCurrentUserPassword(String password) {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findUserByName(user.getName());
        if (userEntity == null) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB,
                            String.format("Unable to find user by name %s", user.getName())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        PasswordEncoder passwordEncoder = SwPasswordEncoder.getEncoder(userEntity.getUserPwdSalt());
        return passwordEncoder.matches(password, userEntity.getUserPwd());
    }


    public List<SystemRoleVo> listSystemRoles() {
        List<ProjectRoleEntity> entities = projectRoleMapper.listSystemRoles();

        return entities.stream()
                .map(systemRoleConvertor::convert)
                .collect(Collectors.toList());
    }

    public List<RoleVo> listRoles() {
        return roleMapper.listRoles()
                .stream()
                .map(roleConvertor::convert)
                .collect(Collectors.toList());
    }

    public List<UserRoleVo> listCurrentUserRoles(String projectUrl) {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findUserByName(user.getName());
        if (userEntity == null) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB,
                            String.format("Unable to find user by name %s", user.getName())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return listUserRoles(userEntity.getId(), projectUrl);
    }

    public List<UserRoleVo> listUserRoles(Long userId, String projectUrl) {
        Long projectId = null;
        if (!StrUtil.isEmpty(projectUrl)) {
            projectId = projectManager.getProjectId(projectUrl);
        }

        List<ProjectRoleEntity> entities = projectRoleMapper.listUserRoles(userId,
                projectId);

        return entities.stream()
                .map(userRoleConvertor::convert)
                .collect(Collectors.toList());
    }

}
