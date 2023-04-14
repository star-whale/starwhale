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

import static ai.starwhale.mlops.domain.bundle.BundleManager.BUNDLE_NAME_REGEX;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelTagRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUploadRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelViewVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.model.RevertModelVersionRequest;
import ai.starwhale.mlops.api.protocol.storage.FileDesc;
import ai.starwhale.mlops.api.protocol.storage.FileNode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public interface ModelApi {

    @Operation(summary = "Get the list of models")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/model",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ModelVo>>> listModel(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Model versionId", schema = @Schema())
            @Valid
            @RequestParam(value = "versionId", required = false)
            String versionId,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Model name prefix to search for",
                    schema = @Schema())
            @Valid @RequestParam(value = "name", required = false) String name,
            @Valid @RequestParam(value = "owner", required = false) String owner,
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
                    "Select a historical version of the model and revert the latest version of the current model to "
                            + "this version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/revert",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> revertModelVersion(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Model Url",
                    required = true,
                    schema = @Schema())
            @PathVariable("modelUrl")
            String modelUrl,
            @Valid
            @RequestBody RevertModelVersionRequest revertRequest);

    @Operation(summary = "Delete a model")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(
            value = "/project/{projectUrl}/model/{modelUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteModel(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl")
                    String modelUrl);

    @Operation(summary = "Recover a model")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/recover",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverModel(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl")
                    String modelUrl);

    @Operation(summary = "Model information",
            description = "Return the file information in the model package of the latest version of the current model")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/model/{modelUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ModelInfoVo>> getModelInfo(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl")
            String modelUrl,
            @Parameter(in = ParameterIn.QUERY,
                    description = "Model versionUrl. "
                            + "(Return the current version as default when the versionUrl is not set.)",
                    schema = @Schema())
            @Valid
            @RequestParam(value = "versionUrl", required = false)
            String versionUrl);


    @Operation(summary = "Model Diff information",
            description = "Return the diff information between the base version and the compare version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/diff",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Map<String, List<FileNode>>>> getModelDiff(
            @Parameter(in = ParameterIn.PATH, description = "Project Url", schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl")
            String modelUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Model version of base. ", schema = @Schema())
            @Valid
            @RequestParam(value = "baseVersion")
            String baseVersion,
            @Parameter(in = ParameterIn.QUERY, description = "Model version of compare. ", schema = @Schema())
            @Valid
            @RequestParam(value = "compareVersion")
            String compareVersion);

    @Operation(summary = "Get the list of model versions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ModelVersionVo>>> listModelVersion(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, description = "Model Url", required = true, schema = @Schema())
            @PathVariable("modelUrl")
            String modelUrl,
            @Parameter(in = ParameterIn.QUERY,
                    description = "Model version name prefix to search for",
                    schema = @Schema())
            @Valid
            @RequestParam(value = "name", required = false)
            String name,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Model version tag",
                    schema = @Schema())
            @RequestParam(value = "tag", required = false)
            String tag,
            @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
            @Valid
            @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
            @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
            @Valid
            @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(summary = "Share or unshare the model version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/shared",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> shareModelVersion(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl") String modelUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl,
            @Parameter(
                in = ParameterIn.QUERY,
                required = true,
                description = "1 or true - shared, 0 or false - unshared",
                schema = @Schema())
            @RequestParam(value = "shared") Boolean shared
    );

    @Operation(summary = "List Model tree including global models")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/model-tree",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ModelViewVo>>> listModelTree(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl);

    @Operation(summary = "Set tag of the model version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyModel(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl")
            String modelUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl")
            String versionUrl,
            @Valid @RequestBody ModelTagRequest modelTagRequest);

    @Operation(summary = "Manage tag of the model version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> manageModelTag(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl")
            String modelUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl")
            String versionUrl,
            @Valid @RequestBody ModelTagRequest modelTagRequest);

    @Operation(summary = "Create a new model version",
            description = "Create a new version of the model. "
                    + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/model/{modelName}/version/{versionName}/file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<Object>> upload(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is invalid.")
            @PathVariable("modelName") String modelName,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionName") String versionName,
            @Parameter(description = "file detail") @RequestPart(value = "file", required = false) MultipartFile file,
            ModelUploadRequest uploadRequest);

    @Operation(summary = "Pull file of a model version",
            description = "Create a new version of the model. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/file",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    void pull(
            @RequestParam(name = "desc", required = false) FileDesc fileDesc,
            @RequestParam(name = "partName", required = false) String name,
            @RequestParam(name = "path", required = false) String path,
            @RequestParam(name = "signature", required = false) String signature,
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is not invalid.")
            @PathVariable("modelUrl") String modelUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl,
            HttpServletResponse httpResponse);


    @Operation(summary = "head for model info ",
            description = "head for model info")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headModel(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("modelUrl") String modelUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl);


}
