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
import ai.starwhale.mlops.api.protocol.swds.SWDSTagRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDatasetInfoVO;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadResult;
import ai.starwhale.mlops.api.protocol.swmp.ClientSWMPRequest;
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
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @Operation(summary = "Recover a dataset")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/recover",
        produces = {"application/json"})
    ResponseEntity<ResponseMessage<String>> recoverDataset(
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
        @RequestParam(value = "name", required = false)
            String name,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Dataset version tag",
            schema = @Schema())
        @RequestParam(value = "tag", required = false)
        String tag,
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
        @Parameter(name = "project", description = "the name of the project attempt to pull", required = true) String project,
        @Parameter(name = "name", description = "the name of the SWDS attempt to pull", required = true) String name,
        @Parameter(name = "version", description = "the version of the SWDS attempt to pull", required = true) String version,
        @Parameter(name = "part_name", description = "optional, _manifest.yaml is used if not specified") @RequestParam(name = "part_name",required = false) String partName,
        HttpServletResponse httpResponse);

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
        @Valid @RequestBody SWDSTagRequest swdsTagRequest);

    @Operation(
        summary = "Manage tag of the dataset version",
        description = "add|remove|set tags"
    )
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag")
    ResponseEntity<ResponseMessage<String>> manageDatasetTag(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project url",
            schema = @Schema())
        @PathVariable("projectUrl")
        String projectUrl,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("datasetUrl")
        String datasetUrl,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("versionUrl")
        String versionUrl,
        @Valid @RequestBody SWDSTagRequest swdsTagRequest);

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

    @Operation(summary = "head for swds info ",
        description = "head for swds info")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
        value = "/project/dataset",
        produces = {"application/json"},
        method = RequestMethod.HEAD)
    ResponseEntity<String> headDataset(UploadRequest uploadRequest);

}
