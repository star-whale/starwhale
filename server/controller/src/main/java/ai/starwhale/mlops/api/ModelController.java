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

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.bundle.DataScope;
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
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "Model")
@RequestMapping("${sw.controller.api-prefix}")
public class ModelController {

    private final ModelService modelService;
    private final IdConverter idConvertor;

    public ModelController(ModelService modelService, IdConverter idConvertor) {
        this.modelService = modelService;
        this.idConvertor = idConvertor;
    }

    @GetMapping(value = "/project/{projectUrl}/model", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ModelVo>>> listModel(
            @PathVariable String projectUrl,
            @RequestParam(required = false) String versionId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<ModelVo> pageInfo;
        if (StringUtils.hasText(versionId)) {
            List<ModelVo> voList = modelService
                    .findModelByVersionId(Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                            Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = modelService.listModel(
                    ModelQuery.builder()
                            .projectUrl(projectUrl)
                            .name(name)
                            .owner(owner)
                            .build(),
                    PageParams.builder()
                            .pageNum(pageNum)
                            .pageSize(pageSize)
                            .build()
            );
        }
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @PostMapping(value = "/project/{projectUrl}/model/{modelUrl}/revert", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> revertModelVersion(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestBody RevertModelVersionRequest revertRequest
    ) {
        Boolean res = modelService.revertVersionTo(projectUrl, modelUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Revert model version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @DeleteMapping(value = "/project/{projectUrl}/model/{modelUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl
    ) {
        Boolean res = modelService.deleteModel(ModelQuery.builder()
                .projectUrl(projectUrl)
                .modelUrl(modelUrl)
                .build());
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Delete model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/recover", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl
    ) {
        Boolean res = modelService.recoverModel(projectUrl, modelUrl);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Recover model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ModelInfoVo>> getModelInfo(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestParam(required = false) String versionUrl
    ) {
        ModelInfoVo modelInfo = modelService.getModelInfo(
                ModelQuery.builder()
                        .projectUrl(projectUrl)
                        .modelUrl(modelUrl)
                        .modelVersionUrl(versionUrl)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(modelInfo));
    }

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Map<String, List<FileNode>>>> getModelDiff(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestParam String baseVersion,
            @Valid @RequestParam String compareVersion
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                modelService.getModelDiff(projectUrl, modelUrl, baseVersion, compareVersion)));
    }

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}/version", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ModelVersionVo>>> listModelVersion(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @Valid @RequestParam(required = false) String name,
            @Valid @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Valid @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<ModelVersionVo> pageInfo = modelService.listModelVersionHistory(
                ModelVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .modelUrl(modelUrl)
                        .versionName(name)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build()
        );
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/shared",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> shareModelVersion(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @RequestParam Boolean shared
    ) {
        modelService.shareModelVersion(projectUrl, modelUrl, versionUrl, shared);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/model-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ModelViewVo>>> listModelTree(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Data range", schema = @Schema())
            @RequestParam(required = false, defaultValue = "all") DataScope scope,
            @RequestParam(required = false) BizType bizType,
            @RequestParam(required = false) Long bizId
    ) {
        List<ModelViewVo> list;
        switch (scope) {
            case all:
                list = modelService.listModelVersionView(projectUrl, true, true, bizType, bizId);
                break;
            case shared:
                list = modelService.listModelVersionView(projectUrl, true, false, bizType, bizId);
                break;
            case project:
                list = modelService.listModelVersionView(projectUrl, false, true, bizType, bizId);
                break;
            default:
                list = List.of();
        }
        return ResponseEntity.ok(Code.success.asResponse(list));
    }

    @GetMapping(value = "/project/{projectUrl}/recent-model-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ModelViewVo>>> recentModelTree(
            @PathVariable String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Data limit", schema = @Schema())
            @RequestParam(required = false, defaultValue = "5")
            @Valid
            @Min(value = 1, message = "limit must be greater than or equal to 1")
            @Max(value = 50, message = "limit must be less than or equal to 50")
            Integer limit,
            @RequestParam(required = false) BizType bizType,
            @RequestParam(required = false) Long bizId
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                modelService.listRecentlyModelVersionView(projectUrl, limit, bizType, bizId)
        ));
    }

    @PutMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody ModelUpdateRequest modelUpdateRequest
    ) {
        Boolean res = modelService.modifyModelVersion(
                projectUrl,
                modelUrl,
                versionUrl,
                ModelVersion.builder()
                        .tag(modelUpdateRequest.getTag())
                        .builtInRuntime(modelUpdateRequest.getBuiltInRuntime())
                        .build()
        );
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Update model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @PostMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> addModelVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody ModelTagRequest modelTagRequest
    ) {
        modelService.addModelVersionTag(
                projectUrl,
                modelUrl,
                versionUrl,
                modelTagRequest.getTag(),
                modelTagRequest.getForce()
        );
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<String>>> listModelVersionTags(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl
    ) {
        var tags = modelService.listModelVersionTags(projectUrl, modelUrl, versionUrl);
        return ResponseEntity.ok(Code.success.asResponse(tags));
    }

    @DeleteMapping(value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteModelVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl,
            @PathVariable String tag
    ) {
        modelService.deleteModelVersionTag(projectUrl, modelUrl, versionUrl, tag);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/model/{modelUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Long>> getModelVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String tag
    ) {
        var entity = modelService.getModelVersionTag(projectUrl, modelUrl, tag);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Code.success.asResponse(entity.getVersionId()));
    }


    @PostMapping(value = "/blob", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseMessage<InitUploadBlobResult>> initUploadBlob(
            @Valid @RequestBody InitUploadBlobRequest initUploadBlobRequest
    ) {
        var result = this.modelService.initUploadBlob(initUploadBlobRequest);
        return ResponseEntity.ok(Code.success.asResponse(result));
    }

    @PostMapping(value = "/blob/{blobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseMessage<CompleteUploadBlobResult>> completeUploadBlob(
            @PathVariable String blobId
    ) {
        var result = this.modelService.completeUploadBlob(blobId);
        return ResponseEntity.ok(Code.success.asResponse(CompleteUploadBlobResult.builder()
                .blobId(result)
                .build()));
    }

    @PostMapping(value = "/project/{project}/model/{modelName}/version/{version}/completeUpload",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    void createModelVersion(
            @PathVariable String project,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Model name is invalid.")
            @PathVariable String modelName,
            @PathVariable String version,
            @Valid @RequestBody CreateModelVersionRequest createModelVersionRequest
    ) {
        this.modelService.createModelVersion(project, modelName, version, createModelVersionRequest);
    }

    @GetMapping(value = "/project/{project}/model/{model}/version/{version}/meta",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> getModelMetaBlob(
            @PathVariable String project,
            @PathVariable String model,
            @PathVariable String version,
            @RequestParam(required = false, defaultValue = "") String blobId
    ) {
        var root = this.modelService.getModelMetaBlob(project, model, version, blobId);
        try {
            return ResponseEntity.ok(Code.success.asResponse(JsonFormat.printer().print(root)));
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.SYSTEM, "failed to print protobuf", e);
        }
    }

    @GetMapping(value = "/project/{project}/model/{model}/listFiles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ListFilesResult>> listFiles(
            @PathVariable String project,
            @PathVariable String model,
            @RequestParam(required = false, defaultValue = "latest") String version,
            @RequestParam(required = false, defaultValue = "") String path
    ) {
        var result = this.modelService.listFiles(project, model, version, path);
        return ResponseEntity.ok(Code.success.asResponse(result));
    }

    @GetMapping(value = "/project/{project}/model/{model}/getFileData",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<InputStreamResource> getFileData(
            @PathVariable String project,
            @PathVariable String model,
            @RequestParam(required = false, defaultValue = "latest") String version,
            @RequestParam String path
    ) {
        var result = this.modelService.getFileData(project, model, version, path);
        return ResponseEntity.ok(new InputStreamResource(result) {
            @Override
            public long contentLength() {
                return result.getSize();
            }
        });
    }

    @RequestMapping(
            value = "/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headModel(
            @PathVariable String projectUrl,
            @PathVariable String modelUrl,
            @PathVariable String versionUrl
    ) {
        try {
            modelService.query(projectUrl, modelUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head model result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }
}
