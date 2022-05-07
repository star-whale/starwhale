/**
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
import ai.starwhale.mlops.api.protocol.swmp.ClientSWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.RevertSWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Model")
@Validated
public interface SWModelPackageApi {

    @Operation(summary = "Get the list of models")
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
    @GetMapping(value = "/project/{projectId}/model")
    ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.QUERY, description = "Model versionId", schema = @Schema())
        @Valid
        @RequestParam(value = "versionId", required = false)
            String versionId,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Model name prefix to search for",
            schema = @Schema())
        @Valid
        @RequestParam(value = "modelName", required = false)
            String modelName,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(
        summary = "Revert model version",
        description =
            "Select a historical version of the model and revert the latest version of the current model to this version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/project/{projectId}/model/{modelId}/revert")
    ResponseEntity<ResponseMessage<String>> revertModelVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Model ID",
            required = true,
            schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Valid
        @RequestBody RevertSWMPVersionRequest revertRequest);

    @Operation(summary = "Delete a model")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/project/{projectId}/model/{modelId}")
    ResponseEntity<ResponseMessage<String>> deleteModelById(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("modelId")
            String modelId);

    @Operation(summary = "Model information",
        description = "Return the file information in the model package of the latest version of the current model")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SWModelPackageInfoVO.class)))
        })
    @GetMapping(value = "/project/{projectId}/model/{modelId}")
    ResponseEntity<ResponseMessage<SWModelPackageInfoVO>> getModelInfo(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Parameter(in = ParameterIn.QUERY, description = "Model versionId. (Return the current version as default when the versionId is not set.)", schema = @Schema())
        @Valid
        @RequestParam(value = "versionId", required = false)
        String versionId);

    @Operation(summary = "Get the list of model versions")
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
    @GetMapping(value = "/project/{projectId}/model/{modelId}/version")
    ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVersionVO>>> listModelVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, description = "Model ID", required = true, schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Parameter(in = ParameterIn.QUERY, description = "Model version name prefix to search for", schema = @Schema())
        @Valid
        @RequestParam(value = "modelVersionName", required = false)
            String modelVersionName,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(summary = "Create a new dataset version",
        description = "Create a new model version. "
            + "The model file can be selected by uploading the file package or entering the server path")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/{projectId}/model/{modelId}/version",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> createModelVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Model id",
            schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Parameter(description = "file detail") @Valid @RequestPart("zipFile") MultipartFile zipFile,
        SWMPVersionRequest swmpVersionRequest);

    @Operation(summary = "Set tag of the model version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectId}/model/{modelId}/version/{versionId}")
    ResponseEntity<ResponseMessage<String>> modifyModel(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("versionId")
            String versionId,
        @Parameter(in = ParameterIn.QUERY, schema = @Schema())
        @Valid
        @RequestParam(value = "tag", required = false)
            String tag);

    @Operation(summary = "Create a new model",
        description = "Create a new model and create an initial version. "
            + "The model file is selected by uploading a file package or entering a server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/{projectId}/model",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> createModel(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(description = "file detail") @RequestPart("zipFile") MultipartFile zipFile,
        SWMPRequest swmpRequest);

    @Operation(summary = "Create a new swmp version",
        description = "Create a new version of the swmp. "
            + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/model/push",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> upload(
        @Parameter(description = "file detail") @RequestPart(value = "file") MultipartFile file,
        ClientSWMPRequest uploadRequest);

    @Operation(summary = "Create a new swmp version",
        description = "Create a new version of the swmp. "
            + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
        value = "/project/model/pull",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] pull(
        ClientSWMPRequest uploadRequest);

    @Operation(summary = "Create a new swmp version",
        description = "Create a new version of the swmp. "
            + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/project/model",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> uploadModel(
        @Parameter(description = "file detail") @RequestPart(value = "file") MultipartFile file,
        ClientSWMPRequest uploadRequest);

    @Operation(summary = "Pull SWMP binary ",
        description = "Pull SWMP binary")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
        value = "/project/model",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    byte[] pullModel(ClientSWMPRequest uploadRequest);

    @Operation(summary = "head for swmp info ",
        description = "head for swmp info")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
        value = "/project/model",
        produces = {"application/json"},
        method = RequestMethod.HEAD)
    ResponseEntity<String> headModel(ClientSWMPRequest uploadRequest);


}
