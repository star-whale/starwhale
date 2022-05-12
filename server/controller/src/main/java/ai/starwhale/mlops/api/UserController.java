/**
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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.IDConvertor;
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

    @Resource
    private IDConvertor idConvertor;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<UserVO>>> listUser(String userName,
        Integer pageNum, Integer pageSize) {
        PageInfo<UserVO> pageInfo = userService.listUsers(User.builder().name(userName).build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());

        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createUser(UserRequest request) {
        Long userId = userService.createUser(User.builder().name(request.getUserName()).build(),
            request.getUserPwd());

        //create default project
        projectService.createProject(Project.builder()
                .name(request.getUserName())
                .owner(User.builder().id(userId).build())
                .isDefault(true)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(userId)));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVO>> getCurrentUser() {
        UserVO userVO = userService.currentUser();
        return ResponseEntity.ok(Code.success.asResponse(userVO));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVO>> getUserById(String userId) {
        UserVO userVO = userService.findUserById(idConvertor.revert(userId));
        return ResponseEntity.ok(Code.success.asResponse(userVO));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserPwd(String userId, String userPwd) {
        Boolean res = userService.changePassword(User.builder().id(idConvertor.revert(userId)).build(), userPwd);
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserState(String userId,
        Boolean isEnabled) {
        Boolean res = userService.updateUserState(User.builder().id(idConvertor.revert(userId)).build(),
            isEnabled);
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }
}
