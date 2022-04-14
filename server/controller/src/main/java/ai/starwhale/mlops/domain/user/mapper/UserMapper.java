/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.user.mapper;

import ai.starwhale.mlops.domain.user.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    int createUser(UserEntity user);

    UserEntity findUser(@Param("id") Long id);

    UserEntity findUserByName(@Param("userName") String userName);

    List<UserEntity> listUsers(@Param("userNamePrefix") String userNamePrefix);

    int changePassword(UserEntity user);

    int enableUser(UserEntity user);
}
