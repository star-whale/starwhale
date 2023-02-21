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
import ai.starwhale.mlops.api.protocol.user.ProjectMemberVo;
import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.UserCheckPasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleAddRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleDeleteRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleUpdateRequest;
import ai.starwhale.mlops.api.protocol.user.UserUpdatePasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserUpdateStateRequest;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwAuthException.AuthType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.api-prefix}")
public class UserController implements UserApi {

    private final UserService userService;

    private final ProjectService projectService;

    private final MemberService memberService;

    private final IdConverter idConvertor;

    private final JwtTokenUtil jwtTokenUtil;

    public UserController(UserService userService, ProjectService projectService,
            MemberService memberService,
            IdConverter idConvertor, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.projectService = projectService;
        this.memberService = memberService;
        this.idConvertor = idConvertor;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<UserVo>>> listUser(String userName,
            Integer pageNum, Integer pageSize) {
        PageInfo<UserVo> pageInfo = userService.listUsers(User.builder().name(userName).build(),
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

        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(userId)));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVo>> getCurrentUser() {
        UserVo userVo = userService.currentUser();
        return ResponseEntity.ok(Code.success.asResponse(userVo));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> getCurrentUserRoles(String projectUrl) {
        List<ProjectMemberVo> vos = projectService.listProjectMemberOfCurrentUser(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Override
    public ResponseEntity<ResponseMessage<UserVo>> getUserById(String userId) {
        UserVo userVo = userService.findUserById(idConvertor.revert(userId));
        return ResponseEntity.ok(Code.success.asResponse(userVo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserPwd(String userId,
            UserUpdatePasswordRequest userUpdatePasswordRequest) {
        if (!userService.checkCurrentUserPassword(userUpdatePasswordRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                    HttpStatus.FORBIDDEN);
        }

        Boolean res = userService.changePassword(User.builder().id(idConvertor.revert(userId)).build(),
                userUpdatePasswordRequest.getNewPwd());
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
        if (!userService.checkCurrentUserPassword(userRoleAddRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                    HttpStatus.FORBIDDEN);
        }
        Boolean res = memberService.addProjectMember(0L, idConvertor.revert(userRoleAddRequest.getUserId()),
                idConvertor.revert(userRoleAddRequest.getRoleId()));
        if (!res) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.USER, "Add user role failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateUserSystemRole(String systemRoleId,
            UserRoleUpdateRequest userRoleUpdateRequest) {
        if (!userService.checkCurrentUserPassword(userRoleUpdateRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                    HttpStatus.FORBIDDEN);
        }
        Boolean res = memberService.modifyProjectMember(idConvertor.revert(systemRoleId),
                idConvertor.revert(
                        userRoleUpdateRequest.getRoleId()));
        if (!res) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.USER, "Update user role failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteUserSystemRole(String systemRoleId,
            UserRoleDeleteRequest userRoleDeleteRequest) {
        if (!userService.checkCurrentUserPassword(userRoleDeleteRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                    HttpStatus.FORBIDDEN);
        }
        Boolean res = memberService.deleteProjectMember(idConvertor.revert(systemRoleId));
        if (!res) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.USER, "Delete user role failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<RoleVo>>> listRoles() {
        return ResponseEntity.ok(Code.success.asResponse(userService.listRoles()));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> listSystemRoles() {
        List<ProjectMemberVo> vos = projectService.listProjectMembersInProject("0");
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> checkCurrentUserPassword(
            UserCheckPasswordRequest userCheckPasswordRequest) {
        if (userService.checkCurrentUserPassword(userCheckPasswordRequest.getCurrentUserPwd())) {
            return ResponseEntity.ok(Code.success.asResponse("success"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Code.accessDenied.asResponse("Incorrect password."));
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateCurrentUserPassword(
            UserUpdatePasswordRequest userUpdatePasswordRequest) {
        if (!userService.checkCurrentUserPassword(userUpdatePasswordRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
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
