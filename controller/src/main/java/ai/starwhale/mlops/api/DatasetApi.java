/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.domain.swds.DatasetVO;
import ai.starwhale.mlops.domain.swds.DatasetVersionVO;
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

@Tag(name = "Dataset")
@Validated
public interface DatasetApi {
    @Operation(summary = "恢复数据集版本", description = "指定一个数据集历史版本，并将当前数据集的最新版本回复至此版本")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/dataset/{datasetId}/revert")
    ResponseEntity<ResponseMessage<String>> revertDatasetVersion(@Parameter(in = ParameterIn.PATH, description = "数据集id", required=true, schema=@Schema()) @PathVariable("datasetId") String datasetId,
        @NotNull @Parameter(in = ParameterIn.QUERY, description = "要回复至的数据集版本id" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "versionId") String versionId);


    @Operation(summary = "删除数据集")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/dataset/{datasetId}")
    ResponseEntity<ResponseMessage<String>> deleteDatasetById(@Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("datasetId") Integer datasetId);


    @Operation(summary = "数据集详情（需求待细化）", description = "这里返回的是当前数据集最新版本的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatasetVersionVO.class))) })
    @GetMapping(value = "/dataset/{datasetId}")
    ResponseEntity<ResponseMessage<DatasetVersionVO>> getDatasetInfo(@Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("datasetId") Integer datasetId);


    @Operation(summary = "获取数据集版本列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class))) })
    @GetMapping(value = "/dataset/{datasetId}/version")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(@Parameter(in = ParameterIn.PATH, description = "要获取版本列表的数据集id", required=true, schema=@Schema()) @PathVariable("datasetId") Integer datasetId,
        @Parameter(in = ParameterIn.QUERY, description = "分页页码" ,schema=@Schema()) @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "每页记录数" ,schema=@Schema()) @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "新建数据集版本", description = "创建新数据集版本，数据资源采用上传文件包或输入服务端路径二选一的方式。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PostMapping(value = "/dataset/{datasetId}/version",
        produces = { "application/json" },
        consumes = { "multipart/form-data" })
    ResponseEntity<Void> createDatasetVersion(@Parameter(in = ParameterIn.PATH, description = "版本所属的数据集id", required=true, schema=@Schema()) @PathVariable("datasetId") String datasetId,
        @Parameter(description = "file detail") @Valid @RequestPart("file") MultipartFile zipFile,
        @Parameter(in = ParameterIn.DEFAULT,schema=@Schema()) @RequestParam(value="importPath", required=false)  String importPath);


    @Operation(summary = "设置数据集版本标签属性")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PutMapping(value = "/dataset/{datasetId}/version/{versionId}")
    ResponseEntity<Void> modifyDatasetVersionInfo(@Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("datasetId") String datasetId,
        @Parameter(in = ParameterIn.PATH, required=true, schema=@Schema()) @PathVariable("versionId") String versionId,
        @NotNull @Parameter(in = ParameterIn.QUERY ,required=true,schema=@Schema()) @Valid @RequestParam(value = "tag") String tag);


    @Operation(summary = "获取数据集列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class))) })
    @GetMapping(value = "/dataset")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(@Parameter(in = ParameterIn.QUERY, description = "分页页码" ,schema=@Schema()) @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "每页记录数" ,schema=@Schema()) @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "新建数据集", description = "创建新数据集并创建初始版本，数据资源采用上传文件包或输入服务端路径二选一的方式。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok") })
    @PostMapping(value = "/dataset",
        produces = { "application/json" },
        consumes = { "multipart/form-data" })
    ResponseEntity<ResponseMessage<String>> createDataset(@Parameter(in = ParameterIn.DEFAULT, required=true,schema=@Schema()) @RequestParam(value="datasetName")  String datasetName,
        @Parameter(description = "file detail") @Valid @RequestPart("file") MultipartFile zipFile,
        @Parameter(in = ParameterIn.DEFAULT, required=true,schema=@Schema()) @RequestParam(value="importPath")  String importPath);

}
