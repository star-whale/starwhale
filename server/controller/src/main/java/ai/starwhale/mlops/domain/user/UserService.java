/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.configuration.security.SWPasswordEncoder;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private SaltGenerator saltGenerator;

    @Resource
    private IDConvertor idConvertor;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findUserByName(username);
        if(userEntity == null) {
            throw new UsernameNotFoundException(String.format("User %s is not found.", username));
        }
        return new User().fromEntity(userEntity, idConvertor);
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

    public UserVO findUserById(String id) {
        UserEntity entity = userMapper.findUser(idConvertor.revert(id));
        return userConvertor.convert(entity);
    }


    public PageInfo<UserVO> listUsers(User user, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<UserEntity> userEntities = userMapper.listUsers(user.getName());

        return PageUtil.toPageInfo(userEntities, userConvertor::convert);
    }

    public String createUser(User user, String rawPassword) {
        String salt = saltGenerator.salt();
        UserEntity userEntity = UserEntity.builder()
            .userName(user.getName())
            .userPwd(SWPasswordEncoder.getEncoder(salt).encode(rawPassword))
            .userPwdSalt(salt)
            .roleId(1L)
            .userEnabled(1)
            .build();
        userMapper.createUser(userEntity);
        log.info("User has been created. ID={}, NAME={}", userEntity.getId(), userEntity.getUserName());
        return idConvertor.convert(userEntity.getId());
    }

    public Boolean changePassword(User user, String newPassword) {
        return changePassword(user, newPassword, null);
    }
    public Boolean changePassword(User user, String newPassword, String oldPassword) {
        String salt = saltGenerator.salt();
        UserEntity userEntity = UserEntity.builder()
            .id(idConvertor.revert(user.getId()))
            .userPwd(SWPasswordEncoder.getEncoder(salt).encode(newPassword))
            .userPwdSalt(salt)
            .build();
        log.info("User password has been changed. ID={}", user.getId());
        return userMapper.changePassword(userEntity) > 0;
    }

    public Boolean updateUserState(User user, Boolean isEnabled) {
        UserEntity userEntity = UserEntity.builder()
            .id(idConvertor.revert(user.getId()))
            .userEnabled(Optional.of(isEnabled).orElse(false) ? 1 : 0)
            .build();
        log.info("User has been {}.", isEnabled ? "enabled" : "disabled");
        return userMapper.enableUser(userEntity) > 0;
    }

}
