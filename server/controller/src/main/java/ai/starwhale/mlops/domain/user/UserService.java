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
import ai.starwhale.mlops.configuration.security.SWPasswordEncoder;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.exception.SWAuthException;
import ai.starwhale.mlops.exception.SWAuthException.AuthType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.lang.Assert;
import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
        User user = new User().fromEntity(userEntity, idConvertor);
        return Assert.notNull(user, () -> new UsernameNotFoundException(String.format("User %s is not found.", username)));
    }


    public UserVO currentUser() {
        User user = currentUserDetail();
        UserEntity userEntity = userMapper.findUserByName(user.getName());
        return userConvertor.convert(userEntity);
    }

    public User currentUserDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            throw new StarWhaleApiException(
                new SWAuthException(AuthType.CURRENT_USER)
                    .tip("Cannot get current user."), HttpStatus.UNAUTHORIZED);
        }

        return (User)authentication.getPrincipal();
    }

    public UserVO findUserById(String id) {
        UserEntity entity = userMapper.findUser(idConvertor.revert(id));
        return userConvertor.convert(entity);
    }


    public List<UserVO> listUsers(User user, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<UserEntity> userEntities = userMapper.listUsers(user.getName());
        return userEntities.stream()
            .map(entity -> userConvertor.convert(entity))
            .collect(Collectors.toList());
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
        return userMapper.changePassword(userEntity) > 0;
    }

    public Boolean updateUserState(User user, Boolean isEnabled) {
        UserEntity userEntity = UserEntity.builder()
            .id(idConvertor.revert(user.getId()))
            .userEnabled(Optional.of(isEnabled).orElse(false) ? 1 : 0)
            .build();
        return userMapper.enableUser(userEntity) > 0;
    }

}
