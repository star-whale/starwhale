/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.domain.node.AgentVO;
import ai.starwhale.mlops.domain.system.SystemVersionVO;
import ai.starwhale.mlops.domain.system.UpgradeProgressVO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "System")
@Validated
public interface SystemApi {
    @Operation(summary = "获取系统节点列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class))) })
    @GetMapping(value = "/system/agent")
    ResponseEntity<ResponseMessage<PageInfo<AgentVO>>> listAgent(@Parameter(in = ParameterIn.QUERY, description = "要查询的节点ip地址，可模糊匹配" ,schema=@Schema()) @Valid @RequestParam(value = "ip", required = false) String ip,
        @Parameter(in = ParameterIn.QUERY, description = "分页页码" ,schema=@Schema()) @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "每页记录数" ,schema=@Schema()) @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "升级或取消升级系统版本")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/system/version/{action}")
    ResponseEntity<ResponseMessage<String>> systemVersionAction(@Parameter(in = ParameterIn.PATH,
                                                        description = "要进行的操作： upgrade：升级系统到最新版本 cancel：取消升级 ",
                                                        required=true,
                                                        schema=@Schema(allowableValues={ "upgrade", "cancel" })) @PathVariable("action") String action);


    @Operation(summary = "获取系统当前版本")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemVersionVO.class))) })
    @GetMapping(value = "/system/version")
    ResponseEntity<ResponseMessage<SystemVersionVO>> getCurrentVersion();


    @Operation(summary = "获取系统最新版本号")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemVersionVO.class)))})
    @GetMapping(value = "/system/version/lastest")
    ResponseEntity<ResponseMessage<SystemVersionVO>> getLatestVersion();


    @Operation(summary = "获取当前升级进度", description = "获取当前服务端升级的进程。如在下载中则返回下载进度")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpgradeProgressVO.class)))})
    @GetMapping(value = "/system/version/progress")
    ResponseEntity<ResponseMessage<UpgradeProgressVO>> getUpgradeProgress();
}
