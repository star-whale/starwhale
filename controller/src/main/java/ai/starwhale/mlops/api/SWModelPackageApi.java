/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.domain.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.domain.swmp.SWModelPackageVO;
import ai.starwhale.mlops.domain.swmp.SWModelPackageVersionVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Model")
@Validated
public interface SWModelPackageApi {

    @Operation(summary = "获取模型列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class))) })
    @GetMapping(value = "/model")
    ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(@Parameter(in = ParameterIn.QUERY, description = "要查询的模型名称前缀" ,schema=@Schema()) @Valid @RequestParam(value = "modelName", required = false) String modelName,
        @Parameter(in = ParameterIn.QUERY, description = "分页页码" ,schema=@Schema()) @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "每页记录数" ,schema=@Schema()) @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "恢复模型版本", description = "指定一个模型历史版本，并将当前模型的最新版本回复至此版本")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/model/{modelId}/revert")
    ResponseEntity<ResponseMessage<String>> revertModelVersion(@Parameter(in = ParameterIn.PATH, description = "模型id", required=true, schema=@Schema()) @PathVariable("modelId") String modelId,
        @NotNull @Parameter(in = ParameterIn.QUERY, description = "要回复至的模型版本id" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "versionId") String versionId);


    @Operation(summary = "删除模型")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/model/{modelId}")
    ResponseEntity<ResponseMessage<String>> deleteModelById(@Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("modelId") Integer modelId);


    @Operation(summary = "模型详情", description = "这里返回的是当前模型最新版本的模型包中的文件信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SWModelPackageInfoVO.class))) })
    @GetMapping(value = "/model/{modelId}")
    ResponseEntity<ResponseMessage<SWModelPackageInfoVO>> getModelInfo(@Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("modelId") Integer modelId);


    @Operation(summary = "获取模型版本列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class)))})
    @GetMapping(value = "/model/{modelId}/version")
    ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVersionVO>>> listModelVersion(@Parameter(in = ParameterIn.PATH, description = "模型id", required=true, schema=@Schema()) @PathVariable("modelId") Integer modelId, @Parameter(in = ParameterIn.QUERY, description = "要查询的模型版本名称前缀" ,schema=@Schema()) @Valid @RequestParam(value = "modelVersionName", required = false) String modelVersionName,
        @Parameter(in = ParameterIn.QUERY, description = "分页页码" ,schema=@Schema()) @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "每页记录数" ,schema=@Schema()) @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "新建模型版本", description = "创建新模型版本，模型文件采用上传文件包或输入服务端路径二选一的方式。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PostMapping(value = "/model/{modelId}/version",
        produces = { "application/json" },
        consumes = { "multipart/form-data" })
    ResponseEntity<ResponseMessage<String>> createModelVersion(@Parameter(in = ParameterIn.PATH, description = "模型id", required=true, schema=@Schema()) @PathVariable("modelId") String modelId,
        @Parameter(description = "file detail") @Valid @RequestPart("file") MultipartFile zipFile,
        @Parameter(in = ParameterIn.DEFAULT,schema=@Schema()) @RequestParam(value="importPath", required=false)  String importPath);


    @Operation(summary = "设置模型版本属性（此版本仅支持修改标签）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PutMapping(value = "/model/{modelId}/version/{versionId}")
    ResponseEntity<ResponseMessage<String>> modifyModel(@Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("modelId") String modelId,
        @Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("versionId") String versionId,
        @Parameter(in = ParameterIn.QUERY ,schema=@Schema()) @Valid @RequestParam(value = "tag", required = false) String tag);


    @Operation(summary = "新建模型", description = "创建新模型并创建初始版本，模型文件采用上传文件包或输入服务端路径二选一的方式。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PostMapping(value = "/model",
        produces = { "application/json" },
        consumes = { "multipart/form-data" })
    ResponseEntity<ResponseMessage<String>> createModel(@Parameter(in = ParameterIn.DEFAULT, required=true,schema=@Schema()) @RequestParam(value="modelName")  String modelName,
        @Parameter(description = "file detail") @Valid @RequestPart("file") MultipartFile zipFile,
        @Parameter(in = ParameterIn.DEFAULT, required=true,schema=@Schema()) @RequestParam(value="importPath")  String importPath);

}
