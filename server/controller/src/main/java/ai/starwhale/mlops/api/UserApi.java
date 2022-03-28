/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.user.UserRequest;
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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
    ResponseEntity<ResponseMessage<UserVO>> getCurrentUser();


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
    ResponseEntity<ResponseMessage<String>> updateUserPwd(
        @Parameter(
            in = ParameterIn.PATH,
            description = "User id to change password",
            required = true,
            schema = @Schema())
        @PathVariable("userId")
            String userId,
        @NotNull
        @Parameter(
            in = ParameterIn.QUERY,
            description = "New password",
            required = true,
            schema = @Schema())
        @Valid
        @RequestParam(value = "userPwd")
            String userPwd);

    @Operation(summary = "Enable or disable a user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/user/{userId}/state")
    ResponseEntity<ResponseMessage<String>> updateUserState(
        @Parameter(
            in = ParameterIn.PATH,
            description = "User ID to enable or disable",
            required = true,
            schema = @Schema())
        @PathVariable("userId")
            String userId,
        @NotNull
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Is enabled: ture or false",
            required = true,
            schema = @Schema())
        @Valid
        @RequestParam(value = "isEnabled")
            Boolean isEnabled);
}
