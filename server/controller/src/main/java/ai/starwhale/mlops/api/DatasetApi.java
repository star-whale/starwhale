/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.api.protocol.swds.RevertSWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSVersionRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Dataset")
@Validated
public interface DatasetApi {

    @Operation(
        summary = "Revert Dataset version",
        description =
            "Select a historical version of the dataset and revert the latest version of the current dataset to this version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/project/{projectId}/dataset/{datasetId}/revert")
    ResponseEntity<ResponseMessage<String>> revertDatasetVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Dataset id",
            required = true,
            schema = @Schema())
        @PathVariable("datasetId")
            String datasetId,
        @Valid @RequestBody RevertSWDSRequest revertRequest);

    @Operation(summary = "Delete a dataset")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/project/{projectId}/dataset/{datasetId}")
    ResponseEntity<ResponseMessage<String>> deleteDatasetById(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetId")
            String datasetId);

    @Operation(summary = "Get the information of a dataset",
        description = "Return the information of the latest version of the current dataset")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DatasetVersionVO.class)))
        })
    @GetMapping(value = "/project/{projectId}/dataset/{datasetId}")
    ResponseEntity<ResponseMessage<DatasetVersionVO>> getDatasetInfo(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetId")
            String datasetId);

    @Operation(summary = "Get the list of the dataset versions")
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
    @GetMapping(value = "/project/{projectId}/dataset/{datasetId}/version")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Dataset ID",
            required = true,
            schema = @Schema())
        @PathVariable("datasetId")
            String datasetId,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Dataset version name prefix",
            schema = @Schema())
        @RequestParam(value = "dsVersionName", required = false)
            String dsVersionName,
        @Parameter(in = ParameterIn.QUERY, description = "The page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(summary = "Create a new dataset version",
        description = "Create a new version of the dataset. "
            + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/{projectId}/dataset/{datasetId}/version",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> createDatasetVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Dataset ID",
            required = true,
            schema = @Schema())
        @PathVariable("datasetId")
            String datasetId,
        @Parameter(description = "file detail") @RequestPart(value = "zipFile", required = false) MultipartFile zipFile,
        SWDSVersionRequest swdsVersionRequest);

    @Operation(summary = "Set the tag of the dataset version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectId}/dataset/{datasetId}/version/{versionId}")
    ResponseEntity<ResponseMessage<String>> modifyDatasetVersionInfo(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetId")
            String datasetId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("versionId")
            String versionId,
        @NotNull
        @Parameter(in = ParameterIn.QUERY, required = true, schema = @Schema())
        @Valid
        @RequestParam(value = "tag")
            String tag);

    @Operation(summary = "Get the list of the datasets")
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
    @GetMapping(value = "/project/{projectId}/dataset")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(summary = "Create a new dataset",
        description = "Create a new data set and create an initial version. "
            + "The data resources are selected by uploading a file package or entering a server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/{projectId}/dataset",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> createDataset(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.DEFAULT, required = true, schema = @Schema())
        @RequestParam(value = "datasetName")
            String datasetName,
        @Parameter(description = "file detail") @RequestPart(value = "zipFile", required = false) MultipartFile zipFile,
        SWDSRequest swdsRequest);
}
