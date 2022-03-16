/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.domain.job.BaseImageVO;
import ai.starwhale.mlops.domain.node.DeviceVO;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Env")
@Validated
public interface EnvApi {
    @Operation(summary = "获取基础镜像列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))) })
    @GetMapping(value = "/env/baseImage")
    ResponseEntity<ResponseMessage<List<BaseImageVO>>> listBaseImage(@Parameter(in = ParameterIn.QUERY, description = "要搜索的镜像名称前缀" ,schema=@Schema()) @Valid @RequestParam(value = "imageName", required = false) String imageName);


    @Operation(summary = "获取设备类型列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))) })
    @GetMapping(value = "/env/device")
    ResponseEntity<ResponseMessage<List<DeviceVO>>> listDevice();
}
