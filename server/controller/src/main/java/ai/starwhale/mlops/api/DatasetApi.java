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
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.api.protocol.swds.RevertSWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSVersionRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDatasetInfoVO;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadResult;
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
import javax.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    @PostMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/revert")
    ResponseEntity<ResponseMessage<String>> revertDatasetVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project Url",
            schema = @Schema())
        @PathVariable("projectUrl")
            String projectUrl,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Dataset Url",
            required = true,
            schema = @Schema())
        @PathVariable("datasetUrl")
            String datasetUrl,
        @Valid @RequestBody RevertSWDSRequest revertRequest);

    @Operation(summary = "Delete a dataset")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}")
    ResponseEntity<ResponseMessage<String>> deleteDataset(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project Url",
            schema = @Schema())
        @PathVariable("projectUrl")
            String projectUrl,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetUrl")
            String datasetUrl);

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
                    schema = @Schema(implementation = SWDatasetInfoVO.class)))
        })
    @GetMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}")
    ResponseEntity<ResponseMessage<SWDatasetInfoVO>> getDatasetInfo(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project Url",
            schema = @Schema())
        @PathVariable("projectUrl")
            String projectUrl,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetUrl")
            String datasetUrl,
        @Parameter(in = ParameterIn.QUERY, description = "Dataset versionUrl. (Return the current version as default when the versionUrl is not set.)", schema = @Schema())
        @Valid
        @RequestParam(value = "versionUrl", required = false)
        String versionUrl);

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
    @GetMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/version")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project Url",
            schema = @Schema())
        @PathVariable("projectUrl")
            String projectUrl,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Dataset Url",
            required = true,
            schema = @Schema())
        @PathVariable("datasetUrl")
            String datasetUrl,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Dataset version name prefix",
            schema = @Schema())
        @RequestParam(value = "vName", required = false)
            String vName,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Dataset version tag",
            schema = @Schema())
        @RequestParam(value = "vTag", required = false)
        String vTag,
        @Parameter(in = ParameterIn.QUERY, description = "The page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

//    @Operation(summary = "Create a new dataset version",
//        description = "Create a new version of the dataset. "
//            + "The data resources can be selected by uploading the file package or entering the server path.")
//    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
//    @PostMapping(
//        value = "/project/{projectId}/dataset/{datasetId}/version",
//        produces = {"application/json"},
//        consumes = {"multipart/form-data"})
//    ResponseEntity<ResponseMessage<String>> createDatasetVersion(
//        @Parameter(
//            in = ParameterIn.PATH,
//            description = "Project id",
//            schema = @Schema())
//        @PathVariable("projectId")
//            String projectId,
//        @Parameter(
//            in = ParameterIn.PATH,
//            description = "Dataset ID",
//            required = true,
//            schema = @Schema())
//        @PathVariable("datasetId")
//            String datasetId,
//        @Parameter(description = "file detail") @RequestPart(value = "zipFile", required = false) MultipartFile zipFile,
//        SWDSVersionRequest swdsVersionRequest);

    @Operation(summary = "Create a new dataset version",
        description = "Create a new version of the dataset. "
            + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/dataset/push",
        produces = {"application/json"})
    ResponseEntity<ResponseMessage<UploadResult>> uploadDS(
        @RequestHeader(name = "X-SW-UPLOAD-ID", required = false) String uploadHeader,
        @Parameter(description = "file detail") @RequestPart(value = "file",required = false) MultipartFile dsFile,
        UploadRequest uploadRequest);

    @Operation(summary = "Pull SWDS files",
        description = "Pull SWDS files part by part. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
        value = "/project/dataset/pull",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] pullDS(
        @Parameter(name = "name", description = "the name of the SWDS attempt to pull", required = true) String name,
        @Parameter(name = "version", description = "the version of the SWDS attempt to pull", required = true) String version,
        @Parameter(name = "part_name", description = "optional, _manifest.yaml is used if not specified") @RequestParam(name = "part_name",required = false) String partName);

    @Operation(summary = "List SWDS versions",
        description = "List SWDS versions. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
        value = "/project/dataset/list",
        produces = {"application/json"})
    ResponseEntity<ResponseMessage<List<SWDatasetInfoVO>>> listDS(
        @Parameter(name = "project", description = "the project name") @RequestParam(name = "project",required = false) String project,
        @Parameter(name = "name", description = "the name of SWDS") @RequestParam(name = "name",required = false) String name);

    @Operation(summary = "Set the tag of the dataset version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}")
    ResponseEntity<ResponseMessage<String>> modifyDatasetVersionInfo(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project Url",
            schema = @Schema())
        @PathVariable("projectUrl")
            String projectUrl,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetUrl")
            String datasetUrl,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("versionUrl")
            String versionUrl,
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
    @GetMapping(value = "/project/{projectUrl}/dataset")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project Url",
            schema = @Schema())
        @PathVariable("projectUrl")
            String projectUrl,
        @Parameter(in = ParameterIn.QUERY, description = "Dataset versionId", schema = @Schema())
        @Valid
        @RequestParam(value = "versionId", required = false)
            String versionId,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

//    @Operation(summary = "Create a new dataset",
//        description = "Create a new data set and create an initial version. "
//            + "The data resources are selected by uploading a file package or entering a server path.")
//    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
//    @PostMapping(
//        value = "/project/{projectId}/dataset",
//        produces = {"application/json"},
//        consumes = {"multipart/form-data"})
//    ResponseEntity<ResponseMessage<String>> createDataset(
//        @Parameter(
//            in = ParameterIn.PATH,
//            description = "Project id",
//            schema = @Schema())
//        @PathVariable("projectId")
//            String projectId,
//        @Parameter(in = ParameterIn.DEFAULT, required = true, schema = @Schema())
//        @RequestParam(value = "datasetName")
//            String datasetName,
//        @Parameter(description = "file detail") @RequestPart(value = "zipFile", required = false) MultipartFile zipFile,
//        SWDSRequest swdsRequest);

}
