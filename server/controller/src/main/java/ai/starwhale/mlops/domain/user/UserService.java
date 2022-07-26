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

import ai.starwhale.mlops.api.protocol.user.RoleVO;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVO;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.configuration.security.SWPasswordEncoder;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.mapper.ProjectRoleMapper;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.mapper.RoleMapper;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.exception.SWAuthException;
import ai.starwhale.mlops.exception.SWAuthException.AuthType;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
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

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private SaltGenerator saltGenerator;

    @Resource
    private ProjectRoleMapper projectRoleMapper;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private RoleConvertor roleConvertor;

    @Resource
    private SystemRoleConvertor systemRoleConvertor;


    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findUserByName(username);
        if(userEntity == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found.", username));
        }
        return new User().fromEntity(userEntity);
    }

    public List<Role> getProjectRolesOfUser(User user, String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);

        List<RoleEntity> projectRolesOfUser = roleMapper.getRolesOfProject(
            user.getId(), projectId);

        return projectRolesOfUser.stream()
            .map(entity -> Role.builder()
                .roleName(entity.getRoleName())
                .roleCode(entity.getRoleCode())
                .build())
            .collect(Collectors.toList());
    }

    public UserVO currentUser() {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findUserByName(user.getName());
        if(userEntity == null) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB)
                .tip(String.format("Unable to find user by name %s", user.getName())), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return userConvertor.convert(userEntity);
    }

    public User currentUserDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            throw new StarWhaleApiException(
                new SWAuthException(AuthType.CURRENT_USER)
                    .tip("Unable to get current user."), HttpStatus.UNAUTHORIZED);
        }

        return (User)authentication.getPrincipal();
    }

    public UserVO findUserById(Long id) {
        UserEntity entity = userMapper.findUser(id);
        return userConvertor.convert(entity);
    }


    public PageInfo<UserVO> listUsers(User user, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<UserEntity> userEntities = userMapper.listUsers(user.getName());

        return PageUtil.toPageInfo(userEntities, userConvertor::convert);
    }

    public Long createUser(User user, String rawPassword) {
        String salt = saltGenerator.salt();
        UserEntity userEntity = UserEntity.builder()
            .userName(user.getName())
            .userPwd(SWPasswordEncoder.getEncoder(salt).encode(rawPassword))
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
            .userPwd(SWPasswordEncoder.getEncoder(salt).encode(newPassword))
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
        if(userEntity == null) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB)
                .tip(String.format("Unable to find user by name %s", user.getName())), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        PasswordEncoder passwordEncoder = SWPasswordEncoder.getEncoder(userEntity.getUserPwdSalt());
        return passwordEncoder.matches(password, userEntity.getUserPwd());
    }


    public List<SystemRoleVO> listSystemRoles() {
        List<ProjectRoleEntity> entities = projectRoleMapper.listSystemRoles();

        return entities.stream()
            .map(systemRoleConvertor::convert)
            .collect(Collectors.toList());
    }

    public List<RoleVO> listRoles() {
        return roleMapper.listRoles()
            .stream()
            .map(roleConvertor::convert)
            .collect(Collectors.toList());
    }

}
