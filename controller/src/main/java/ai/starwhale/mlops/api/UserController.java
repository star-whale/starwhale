/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.domain.user.User;
import com.github.pagehelper.PageInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApi{

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<User>>> listUser(String userName,
        Integer pageNum, Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createUser(String userName, String userPwd) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<User>> getUserById(String userId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserPwd(String userId, String userPwd) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserState(String userId,
        Boolean isEnabled) {
        return null;
    }
}
