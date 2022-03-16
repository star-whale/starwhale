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
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User")
@Validated
public interface UserApi {
    @Operation(summary = "获取用户列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "ok",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class))) })
    @GetMapping(value = "/user")
    ResponseEntity<ResponseMessage<PageInfo<User>>> listUser(@Parameter(in = ParameterIn.QUERY, description = "要查询的用户名，可模糊匹配" ,schema=@Schema()) @Valid @RequestParam(value = "userName", required = false) String userName,
        @Parameter(in = ParameterIn.QUERY, description = "分页页码" ,schema=@Schema()) @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "每页记录数" ,schema=@Schema()) @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "新建用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PostMapping(value = "/user")
    ResponseEntity<ResponseMessage<String>> createUser(@NotNull @Parameter(in = ParameterIn.QUERY, description = "用户名" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "userName") String userName,
        @NotNull @Parameter(in = ParameterIn.QUERY, description = "用户密码" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "userPwd") String userPwd);


    @Operation(summary = "根据userId获取用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))) })
    @GetMapping(value = "/user/{userId}")
    ResponseEntity<ResponseMessage<User>> getUserById(@Parameter(in = ParameterIn.PATH, description = "用户id", required=true, schema=@Schema()) @PathVariable("userId") String userId);


    @Operation(summary = "修改用户密码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PutMapping (value = "/user/{userId}/pwd")
    ResponseEntity<ResponseMessage<String>> updateUserPwd(@Parameter(in = ParameterIn.PATH, description = "要修改密码的用户id", required=true, schema=@Schema()) @PathVariable("userId") String userId,
        @NotNull @Parameter(in = ParameterIn.QUERY, description = "修改后的用户密码" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "userPwd") String userPwd);


    @Operation(summary = "启用或禁用用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PutMapping(value = "/user/{userId}/state")
    ResponseEntity<ResponseMessage<String>> updateUserState(@Parameter(in = ParameterIn.PATH, description = "要启用/禁用的用户", required=true, schema=@Schema()) @PathVariable("userId") String userId,
        @NotNull @Parameter(in = ParameterIn.QUERY, description = "是否启用。true为启用，false为禁用" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "isEnabled") Boolean isEnabled);

}
