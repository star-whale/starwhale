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
import ai.starwhale.mlops.api.protocol.dataset.DatasetInfoVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetTagRequest;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.dataset.RevertDatasetRequest;
import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataConsumptionRequest;
import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.api.protocol.dataset.upload.DatasetUploadRequest;
import ai.starwhale.mlops.api.protocol.upload.UploadResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
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

@Tag(name = "Dataset")
@Validated
public interface DatasetApi {

    @Operation(summary = "Revert Dataset version",
            description = "Select a historical version of the dataset and revert the latest version of the current "
                    + "dataset to this version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/revert",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
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
            @Valid @RequestBody RevertDatasetRequest revertRequest);

    @Operation(summary = "Delete a dataset")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
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
    @PutMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/recover",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
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
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<DatasetInfoVo>> getDatasetInfo(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("datasetUrl")
            String datasetUrl,
            @Parameter(in = ParameterIn.QUERY,
                    description = "Dataset versionUrl. "
                            + "(Return the current version as default when the versionUrl is not set.)",
                    schema = @Schema())
            @Valid
            @RequestParam(value = "versionUrl", required = false)
            String versionUrl);

    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/consume",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<DataIndexDesc>> consumeNextData(
            @Parameter(in = ParameterIn.PATH,
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
            @RequestBody
            DataConsumptionRequest dataRangeRequest);

    @Operation(summary = "Get the list of the dataset versions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVo>>> listDatasetVersion(
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

    /**
     * use #uploadHashedBlob instead
     */
    @Operation(summary = "Create a new dataset version",
            description = "Create a new version of the dataset. "
                    + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/dataset/{datasetName}/version/{versionName}/file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    @Deprecated
    ResponseEntity<ResponseMessage<UploadResult>> uploadDs(
            @PathVariable(name = "projectUrl") String projectUrl,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Dataset name is invalid")
            @PathVariable(name = "datasetName") String datasetName,
            @PathVariable(name = "versionName") String versionName,
            @Parameter(description = "file detail") @RequestPart(value = "file", required = false) MultipartFile dsFile,
            DatasetUploadRequest uploadRequest);

    /**
     * legacy blob content download api, use {@link #signBlobLinks} or {@link #pullBlob} instead
     */
    @Operation(summary = "Pull Dataset files",
            description = "Pull Dataset files part by part. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/file",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    @Deprecated
    void pullDs(
            @PathVariable(name = "projectUrl") String projectUrl,
            @PathVariable(name = "datasetUrl") String datasetUrl,
            @PathVariable(name = "versionUrl") String versionUrl,
            @Parameter(name = "partName", description = "optional, _manifest.yaml is used if not specified")
            @RequestParam(name = "partName", required = false) String partName,
            HttpServletResponse httpResponse);


    @Operation(summary = "Upload a hashed BLOB to dataset object store",
            description = "Upload a hashed BLOB to dataset object store, returns a uri of the main storage")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> uploadHashedBlob(
            @PathVariable(name = "projectName") String projectName,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Dataset name is invalid")
            @PathVariable(name = "datasetName") String datasetName,
            @PathVariable(name = "hash") String hash,
            @Parameter(description = "file content") @RequestPart(value = "file", required = true)
            MultipartFile dsFile);

    @Operation(summary = "Pull Dataset uri file contents",
            description = "Pull Dataset uri file contents ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectName}/dataset/{datasetName}/blob",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    void pullBlob(
            @PathVariable(name = "projectName") String projectName,
            @PathVariable(name = "datasetName") String datasetName,
            @Parameter(name = "uri", required = true) String uri,
            @Parameter(name = "offset", description = "offset in the content")
            @RequestParam(name = "offset", required = false) Long offset,
            @Parameter(name = "size", description = "data size")
            @RequestParam(name = "size", required = false) Long size,
            HttpServletResponse httpResponse);

    @Operation(summary = "Sign SWDS uris to get a batch of temporarily accessible links",
            description = "Sign SWDS uris to get a batch of temporarily accessible links")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectName}/dataset/{datasetName}/blob/sign-links",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Map>> signBlobLinks(@PathVariable(name = "projectName") String projectName,
            @PathVariable(name = "datasetName") String datasetName,
            @RequestBody Set<String> uris,
            @Parameter(name = "expTimeMillis", description = "the link will be expired after expTimeMillis")
            @RequestParam(name = "expTimeMillis", required = false) Long expTimeMillis);


    @Operation(
            summary = "Manage tag of the dataset version",
            description = "add|remove|set tags"
    )
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
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
            @Valid @RequestBody DatasetTagRequest datasetTagRequest);

    @Operation(summary = "Get the list of the datasets")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/dataset",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVo>>> listDataset(
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
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Dataset name prefix to search for",
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

    @Operation(summary = "head for dataset info ",
            description = "head for dataset info")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headDataset(
            @Parameter(in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("datasetUrl")
            String datasetUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl")
            String versionUrl);

}
