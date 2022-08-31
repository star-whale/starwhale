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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.user.ProjectRoleVO;
import ai.starwhale.mlops.api.protocol.user.RoleVO;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVO;
import ai.starwhale.mlops.api.protocol.user.UserCheckPasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleDeleteRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleUpdateRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleVO;
import ai.starwhale.mlops.api.protocol.user.UserUpdatePasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleAddRequest;
import ai.starwhale.mlops.api.protocol.user.UserUpdateStateRequest;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWAuthException;
import ai.starwhale.mlops.exception.SWAuthException.AuthType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
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

    @Resource
    JwtTokenUtil jwtTokenUtil;

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
            request.getUserPwd(), request.getSalt());

        //create default project
//        projectService.createProject(Project.builder()
//                .name(request.getUserName())
//                .owner(User.builder().id(userId).build())
//                .isDefault(true)
//                .build());
        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(userId)));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVO>> getCurrentUser() {
        UserVO userVO = userService.currentUser();
        return ResponseEntity.ok(Code.success.asResponse(userVO));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<UserRoleVO>>> getCurrentUserRoles(String projectUrl) {
        List<UserRoleVO> vos = userService.listCurrentUserRoles(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVO>> getUserById(String userId) {
        UserVO userVO = userService.findUserById(idConvertor.revert(userId));
        return ResponseEntity.ok(Code.success.asResponse(userVO));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserPwd(String userId, UserUpdatePasswordRequest userUpdatePasswordRequest) {
        if(!userService.checkCurrentUserPassword(userUpdatePasswordRequest.getCurrentUserPwd())) {
            throw new StarWhaleApiException(new SWAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                HttpStatus.FORBIDDEN);
        }

        Boolean res = userService.changePassword(User.builder().id(idConvertor.revert(userId)).build(), userUpdatePasswordRequest.getNewPwd());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserState(String userId,
        UserUpdateStateRequest userUpdateStateRequest) {
        Boolean res = userService.updateUserState(User.builder().id(idConvertor.revert(userId)).build(),
            userUpdateStateRequest.getIsEnabled());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> addUserSystemRole(
        UserRoleAddRequest userRoleAddRequest) {
        if(!userService.checkCurrentUserPassword(userRoleAddRequest.getCurrentUserPwd())) {
            throw new StarWhaleApiException(new SWAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                HttpStatus.FORBIDDEN);
        }
        Boolean res = projectService.addProjectRole("0", idConvertor.revert(userRoleAddRequest.getUserId()),
            idConvertor.revert(userRoleAddRequest.getRoleId()));
        if(!res) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.USER).tip("Add user role failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserSystemRole(String systemRoleId,
        UserRoleUpdateRequest userRoleUpdateRequest) {
        if(!userService.checkCurrentUserPassword(userRoleUpdateRequest.getCurrentUserPwd())) {
            throw new StarWhaleApiException(new SWAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                HttpStatus.FORBIDDEN);
        }
        Boolean res = projectService.modifyProjectRole("0", idConvertor.revert(systemRoleId),
            idConvertor.revert(
                userRoleUpdateRequest.getRoleId()));
        if(!res) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.USER).tip("Update user role failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteUserSystemRole(String systemRoleId,
        UserRoleDeleteRequest userRoleDeleteRequest) {
        if(!userService.checkCurrentUserPassword(userRoleDeleteRequest.getCurrentUserPwd())) {
            throw new StarWhaleApiException(new SWAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                HttpStatus.FORBIDDEN);
        }
        Boolean res = projectService.deleteProjectRole("0", idConvertor.revert(systemRoleId));
        if(!res) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.USER).tip("Delete user role failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<RoleVO>>> listRoles() {
        return ResponseEntity.ok(Code.success.asResponse(userService.listRoles()));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<SystemRoleVO>>> listSystemRoles() {
        return ResponseEntity.ok(Code.success.asResponse(userService.listSystemRoles()));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> checkCurrentUserPassword(UserCheckPasswordRequest userCheckPasswordRequest) {
        if(userService.checkCurrentUserPassword(userCheckPasswordRequest.getCurrentUserPwd())) {
            return ResponseEntity.ok(Code.success.asResponse("success"));
        } else {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Code.accessDenied.asResponse("Incorrect password."));
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateCurrentUserPassword(UserUpdatePasswordRequest userUpdatePasswordRequest) {
        if(!userService.checkCurrentUserPassword(userUpdatePasswordRequest.getCurrentUserPwd())) {
            throw new StarWhaleApiException(new SWAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                HttpStatus.FORBIDDEN);
        }
        Boolean res = userService.changePassword(userService.currentUserDetail(),
            userUpdatePasswordRequest.getNewPwd());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> userToken(Long userId) {
        User user = userService.loadUserById(userId);
        return ResponseEntity.ok(Code.success.asResponse(jwtTokenUtil.generateAccessToken(user)));
    }
}
