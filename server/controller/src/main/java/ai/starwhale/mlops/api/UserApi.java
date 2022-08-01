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
import ai.starwhale.mlops.api.protocol.user.RoleVO;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVO;
import ai.starwhale.mlops.api.protocol.user.UserCheckPasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleAddRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleDeleteRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleUpdateRequest;
import ai.starwhale.mlops.api.protocol.user.UserRoleVO;
import ai.starwhale.mlops.api.protocol.user.UserUpdatePasswordRequest;
import ai.starwhale.mlops.api.protocol.user.UserUpdateStateRequest;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
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
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageInfo.class)))
        })
    @GetMapping(value = "/user")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<PageInfo<UserVO>>> listUser(
        @Parameter(
            in = ParameterIn.QUERY,
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
    @PostMapping(value = "/user")
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> createUser(@Valid @RequestBody UserRequest request);

    @Operation(summary = "Get the current logged in user.")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok.",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserVO.class)))
        })
    @GetMapping(value = "/user/current")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<UserVO>> getCurrentUser();

    @Operation(summary = "Get the current user roles.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/user/current/role")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<UserRoleVO>>> getCurrentUserRoles(
        @RequestParam(value = "projectUrl", required = false) String projectUrl
    );


    @Operation(summary = "Check Current User password")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/user/current/pwd")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> checkCurrentUserPassword(@RequestBody UserCheckPasswordRequest userCheckPasswordRequest);


    @Operation(summary = "Update Current User password")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/user/current/pwd")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> updateCurrentUserPassword(@RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest);


    @Operation(summary = "Get a user by user ID")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok.",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserVO.class)))
        })
    @GetMapping(value = "/user/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<UserVO>> getUserById(
        @Parameter(
            in = ParameterIn.PATH,
            description = "User ID",
            required = true,
            schema = @Schema())
        @PathVariable("userId")
            String userId);

    @Operation(summary = "Change user password")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/user/{userId}/pwd")
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserPwd(
        @Parameter(
            in = ParameterIn.PATH,
            description = "User id to change password",
            required = true,
            schema = @Schema())
        @PathVariable("userId")
            String userId,
        @Valid @RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest);

    @Operation(summary = "Enable or disable a user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/user/{userId}/state")
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserState(
        @Parameter(
            in = ParameterIn.PATH,
            description = "User ID to enable or disable",
            required = true,
            schema = @Schema())
        @PathVariable("userId")
            String userId,
        @Valid @RequestBody UserUpdateStateRequest userUpdateStateRequest);

    @Operation(summary = "Add user role of system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/role")
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> addUserSystemRole(
        @Valid @RequestBody UserRoleAddRequest userRoleAddRequest);

    @Operation(summary = "Update user role of system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/role/{systemRoleId}")
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateUserSystemRole(
        @Parameter(
            in = ParameterIn.PATH,
            description = "System Role ID to update",
            required = true,
            schema = @Schema())
        @PathVariable("systemRoleId")
        String systemRoleId,
        @Valid @RequestBody UserRoleUpdateRequest userRoleUpdateRequest);

    @Operation(summary = "Delete user role of system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/role/{systemRoleId}")
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> deleteUserSystemRole(
        @Parameter(
            in = ParameterIn.PATH,
            description = "System Role ID to delete",
            required = true,
            schema = @Schema())
        @PathVariable("systemRoleId")
        String systemRoleId,
        @Valid @RequestBody UserRoleDeleteRequest userRoleDeleteRequest);

    @Operation(summary = "List system role of users")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/role")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<SystemRoleVO>>> listSystemRoles();

    @Operation(summary = "List role enums")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/role/enums")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<RoleVO>>> listRoles();
}
