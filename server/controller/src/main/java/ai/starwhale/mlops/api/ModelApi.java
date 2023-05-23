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
import ai.starwhale.mlops.api.protocol.model.CompleteUploadBlobResult;
import ai.starwhale.mlops.api.protocol.model.CreateModelVersionRequest;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobRequest;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobResult;
import ai.starwhale.mlops.api.protocol.model.ListFilesResult;
import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelTagRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUpdateRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelViewVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.model.RevertModelVersionRequest;
import ai.starwhale.mlops.api.protocol.storage.FileNode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.springframework.core.io.InputStreamResource;
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

@Tag(name = "Model")
@Validated
public interface ModelApi {

    @GetMapping(value = "/project/{projectUrl}/model", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ModelVo>>> listModel(
            @PathVariable String projectUrl,
            @Valid @RequestParam(required = false) String versionId,
            @Valid @RequestParam(required = false) String name,
            @Valid @RequestParam(required = false) String owner,
            @Valid @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Valid @RequestParam(required = false, defaultValue = "10") Integer pageSize);

    @PostMapping(value = "/project/{projectUrl}/model/{modelUrl}/revert", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> revertModelVersion(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestBody RevertModelVersionRequest revertRequest);

    @DeleteMapping(value = "/project/{projectUrl}/model/{modelUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl);

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/recover", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl);

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ModelInfoVo>> getModelInfo(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestParam(required = false) String versionUrl);

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Map<String, List<FileNode>>>> getModelDiff(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestParam String baseVersion,
            @Valid @RequestParam String compareVersion);

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}/version", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ModelVersionVo>>> listModelVersion(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestParam(required = false) String name,
            @RequestParam(required = false) String tag,
            @Valid @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Valid @RequestParam(required = false, defaultValue = "10") Integer pageSize);

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/shared",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> shareModelVersion(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @RequestParam Boolean shared);

    @GetMapping(value = "/project/{projectUrl}/model-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ModelViewVo>>> listModelTree(
            @PathVariable String projectUrl);

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody ModelUpdateRequest modelUpdateRequest);

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> manageModelTag(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody ModelTagRequest modelTagRequest);

    @PostMapping(value = "/blob", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseMessage<InitUploadBlobResult>> initUploadBlob(
            @Valid @RequestBody InitUploadBlobRequest initUploadBlobRequest);

    @PostMapping(value = "/blob/{blobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseMessage<CompleteUploadBlobResult>> completeUploadBlob(
            @PathVariable String blobId);

    @PostMapping(value = "/project/{project}/model/{model}/version/{version}/completeUpload",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    void createModelVersion(
            @PathVariable String project,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is invalid.")
            @PathVariable String model,
            @PathVariable String version,
            @Valid @RequestBody CreateModelVersionRequest createModelVersionRequest);

    @GetMapping(value = "/project/{project}/model/{model}/version/{version}/meta",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> getModelMetaBlob(
            @PathVariable String project,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is invalid.") @PathVariable String model,
            @PathVariable String version,
            @RequestParam(required = false, defaultValue = "") String blobId);

    @GetMapping(value = "/project/{project}/model/{model}/listFiles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ListFilesResult>> listFiles(
            @PathVariable String project,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is invalid.") @PathVariable String model,
            @RequestParam(required = false, defaultValue = "latest") String version,
            @RequestParam(required = false, defaultValue = "") String path);

    @GetMapping(value = "/project/{project}/model/{model}/getFileData",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<InputStreamResource> getFileData(
            @PathVariable String project,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is invalid.") @PathVariable String model,
            @RequestParam(required = false, defaultValue = "latest") String version,
            @RequestParam String path);

    @RequestMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl);
}
