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
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User")
@Validated
public interface UserApi {

    @Operation(summary = "Get the list of users")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/user",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<PageInfo<UserVo>>> listUser(
            @Parameter(in = ParameterIn.QUERY,
                    description = "User name prefix to search for",
                    schema = @Schema())
            @Valid
            @RequestParam(value = "userName", required = false)
                    String userName,
            @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
            @Valid
            @RequestParam(value = "pageNum", required = false, defaultValue = "1")
                    Integer pageNum,
            @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
            @Valid
            @RequestParam(value = "pageSize", required = false, defaultValue = "10")
                    Integer pageSize);

    @Operation(summary = "Create a new user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/user",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> createUser(@Valid @RequestBody UserRequest request);

    @Operation(summary = "Get the current logged in user.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/user/current",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<UserVo>> getCurrentUser();

    @Operation(summary = "Get the current user roles.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/user/current/role",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> getCurrentUserRoles(
            @RequestParam("projectUrl") String projectUrl
    );


    @Operation(summary = "Check Current User password")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/user/current/pwd",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> checkCurrentUserPassword(
            @RequestBody UserCheckPasswordRequest userCheckPasswordRequest);


    @Operation(summary = "Update Current User password")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/user/current/pwd",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> updateCurrentUserPassword(
            @RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest);


    @Operation(summary = "Get a user by user ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/user/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<UserVo>> getUserById(
            @Parameter(in = ParameterIn.PATH,
                    description = "User ID",
                    required = true,
                    schema = @Schema())
            @PathVariable("userId")
                    String userId);

    @Operation(summary = "Change user password")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/user/{userId}/pwd",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserPwd(
            @Parameter(in = ParameterIn.PATH,
                    description = "User id to change password",
                    required = true,
                    schema = @Schema())
            @PathVariable("userId")
                    String userId,
            @Valid
            @RequestBody
                    UserUpdatePasswordRequest userUpdatePasswordRequest);

    @Operation(summary = "Enable or disable a user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/user/{userId}/state",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserState(
            @Parameter(in = ParameterIn.PATH,
                    description = "User ID to enable or disable",
                    required = true,
                    schema = @Schema())
            @PathVariable("userId")
                    String userId,
            @Valid
            @RequestBody
                    UserUpdateStateRequest userUpdateStateRequest);

    @Operation(summary = "Add user role of system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/role",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> addUserSystemRole(
            @Valid @RequestBody UserRoleAddRequest userRoleAddRequest);

    @Operation(summary = "Update user role of system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/role/{systemRoleId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserSystemRole(
            @Parameter(in = ParameterIn.PATH,
                    description = "System Role ID to update",
                    required = true,
                    schema = @Schema())
            @PathVariable("systemRoleId")
                    String systemRoleId,
            @Valid
            @RequestBody
                    UserRoleUpdateRequest userRoleUpdateRequest);

    @Operation(summary = "Delete user role of system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(
            value = "/role/{systemRoleId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> deleteUserSystemRole(
            @Parameter(in = ParameterIn.PATH,
                    description = "System Role ID to delete",
                    required = true,
                    schema = @Schema())
            @PathVariable("systemRoleId")
                    String systemRoleId,
            @Valid
            @RequestBody
                    UserRoleDeleteRequest userRoleDeleteRequest);

    @Operation(summary = "List system role of users")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/role",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> listSystemRoles();

    @Operation(summary = "List role enums")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/role/enums",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<RoleVo>>> listRoles();


    @Operation(summary = "Get arbitrary user token",
            description =
                    "Get token of any user for third party system integration, only super admin is allowed to do this")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/user/token/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> userToken(
            @Parameter(in = ParameterIn.PATH,
                    description = "account id of a user",
                    schema = @Schema())
            @PathVariable("userId")
                    Long userId);
}
