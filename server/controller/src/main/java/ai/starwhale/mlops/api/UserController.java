/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.project.Project;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import com.github.pagehelper.PageInfo;
import javax.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class UserController implements UserApi{

    @Resource
    private UserService userService;

    @Resource
    private ProjectService projectService;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<UserVO>>> listUser(String userName,
        Integer pageNum, Integer pageSize) {
        PageInfo<UserVO> pageInfo = userService.listUsers(User.builder().name(userName).build(),
            new PageParams(pageNum, pageSize));

        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createUser(UserRequest request) {
        String id = userService.createUser(User.builder().name(request.getUserName()).build(),
            request.getUserPwd());

        //create default project
        projectService.createProject(Project.builder()
                .name(request.getUserName())
                .ownerId(id)
                .isDefault(true)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(id));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVO>> getCurrentUser() {
        UserVO userVO = userService.currentUser();
        return ResponseEntity.ok(Code.success.asResponse(userVO));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVO>> getUserById(String userId) {
        UserVO userVO = userService.findUserById(userId);
        return ResponseEntity.ok(Code.success.asResponse(userVO));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserPwd(String userId, String userPwd) {
        Boolean res = userService.changePassword(User.builder().id(userId).build(), userPwd);
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserState(String userId,
        Boolean isEnabled) {
        Boolean res = userService.updateUserState(User.builder().id(userId).build(),
            isEnabled);
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }
}
