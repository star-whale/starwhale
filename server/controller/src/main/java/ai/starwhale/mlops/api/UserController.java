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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "User")
@RequestMapping("${sw.controller.api-prefix}")
public class UserController {

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

    @Operation(summary = "Get the list of users")
    @GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<PageInfo<UserVo>>> listUser(
            @Parameter(in = ParameterIn.QUERY, description = "User name prefix to search for")
            @RequestParam(required = false) String userName,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageInfo<UserVo> pageInfo = userService.listUsers(User.builder().name(userName).build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());

        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Operation(summary = "Create a new user")
    @PostMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> createUser(@Valid @RequestBody UserRequest request) {
        Long userId = userService.createUser(User.builder().name(request.getUserName()).build(),
                request.getUserPwd(), request.getSalt());

        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(userId)));
    }

    @Operation(summary = "Get the current logged in user.")
    @GetMapping(value = "/user/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<UserVo>> getCurrentUser() {
        UserVo userVo = userService.currentUser();
        return ResponseEntity.ok(Code.success.asResponse(userVo));
    }

    @Operation(summary = "Get the current user roles.")
    @GetMapping(value = "/user/current/role", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> getCurrentUserRoles(
            @RequestParam String projectUrl
    ) {
        List<ProjectMemberVo> vos = projectService.listProjectMemberOfCurrentUser(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Operation(summary = "Get a user by user ID")
    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<UserVo>> getUserById(@PathVariable String userId) {
        UserVo userVo = userService.findUserById(idConvertor.revert(userId));
        return ResponseEntity.ok(Code.success.asResponse(userVo));
    }

    @Operation(summary = "Change user password")
    @PutMapping(value = "/user/{userId}/pwd", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserPwd(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest
    ) {
        if (!userService.checkCurrentUserPassword(userUpdatePasswordRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                    HttpStatus.FORBIDDEN);
        }

        Boolean res = userService.changePassword(User.builder().id(idConvertor.revert(userId)).build(),
                userUpdatePasswordRequest.getNewPwd());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Operation(summary = "Enable or disable a user")
    @PutMapping(value = "/user/{userId}/state", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserState(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateStateRequest userUpdateStateRequest
    ) {
        Boolean res = userService.updateUserState(User.builder().id(idConvertor.revert(userId)).build(),
                userUpdateStateRequest.getIsEnabled());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Operation(summary = "Add user role of system")
    @PostMapping(value = "/role", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> addUserSystemRole(
            @Valid @RequestBody UserRoleAddRequest userRoleAddRequest
    ) {
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

    @Operation(summary = "Update user role of system")
    @PutMapping(value = "/role/{systemRoleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserSystemRole(
            @PathVariable String systemRoleId,
            @Valid @RequestBody UserRoleUpdateRequest userRoleUpdateRequest
    ) {
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

    @Operation(summary = "Delete user role of system")
    @DeleteMapping(value = "/role/{systemRoleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> deleteUserSystemRole(
            @PathVariable String systemRoleId,
            @Valid @RequestBody UserRoleDeleteRequest userRoleDeleteRequest
    ) {
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

    @Operation(summary = "List role enums")
    @GetMapping(value = "/role/enums", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<RoleVo>>> listRoles() {
        return ResponseEntity.ok(Code.success.asResponse(userService.listRoles()));
    }

    @Operation(summary = "List system role of users")
    @GetMapping(value = "/role", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> listSystemRoles() {
        List<ProjectMemberVo> vos = projectService.listProjectMembersInProject("0");
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Operation(summary = "Check Current User password")
    @PostMapping(value = "/user/current/pwd", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> checkCurrentUserPassword(
            @Valid @RequestBody UserCheckPasswordRequest userCheckPasswordRequest
    ) {
        if (userService.checkCurrentUserPassword(userCheckPasswordRequest.getCurrentUserPwd())) {
            return ResponseEntity.ok(Code.success.asResponse("success"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Code.accessDenied.asResponse("Incorrect password."));
        }
    }

    @Operation(summary = "Update Current User password")
    @PutMapping(value = "/user/current/pwd", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> updateCurrentUserPassword(
            @Valid @RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest
    ) {
        if (!userService.checkCurrentUserPassword(userUpdatePasswordRequest.getCurrentUserPwd())) {
            throw new StarwhaleApiException(
                    new SwAuthException(AuthType.CURRENT_USER).tip("Incorrect current user password."),
                    HttpStatus.FORBIDDEN);
        }
        Boolean res = userService.changePassword(userService.currentUserDetail(),
                userUpdatePasswordRequest.getNewPwd());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(res)));
    }

    @Operation(summary = "Get arbitrary user token",
            description =
                    "Get token of any user for third party system integration, only super admin is allowed to do this")
    @GetMapping(value = "/user/token/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> userToken(@PathVariable Long userId) {
        User user = userService.loadUserById(userId);
        return ResponseEntity.ok(Code.success.asResponse(jwtTokenUtil.generateAccessToken(user)));
    }
}
