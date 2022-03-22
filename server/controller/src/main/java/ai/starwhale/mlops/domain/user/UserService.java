/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.configuration.security.SWPasswordEncoder;
import cn.hutool.core.lang.Assert;
import com.google.common.base.Preconditions;
import java.util.function.Supplier;
import javax.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private SaltGenerator saltGenerator;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findUserByName(username);
        User user = User.fromEntity(userEntity);
        return Assert.notNull(user, () -> new UsernameNotFoundException(String.format("User %s is not found.", username)));
    }


    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            return null;
        }

        return (User)authentication.getPrincipal();
    }

    public Long createUser(UserVO userVO, String rawPassword) {
        UserEntity userEntity = userConvertor.revert(userVO);
        String salt = saltGenerator.salt();
        userEntity.setUserPwd(SWPasswordEncoder.getEncoder(salt).encode(rawPassword));
        userEntity.setUserPwdSalt(salt);
        return userMapper.createUser(userEntity);
    }

}
