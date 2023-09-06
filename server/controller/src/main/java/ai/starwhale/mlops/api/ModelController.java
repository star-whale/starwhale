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

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.bundle.DataRange;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${sw.controller.api-prefix}")
public class ModelController implements ModelApi {

    private final ModelService modelService;
    private final IdConverter idConvertor;

    public ModelController(ModelService modelService, IdConverter idConvertor) {
        this.modelService = modelService;
        this.idConvertor = idConvertor;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ModelVo>>> listModel(
            String projectUrl,
            String versionId,
            String name,
            String owner,
            Integer pageNum,
            Integer pageSize
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
                            .namePrefix(name)
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

    @Override
    public ResponseEntity<ResponseMessage<String>> revertModelVersion(
            String projectUrl,
            String modelUrl,
            RevertModelVersionRequest revertRequest
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

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModel(String projectUrl, String modelUrl) {
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

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverModel(String projectUrl, String modelUrl) {
        Boolean res = modelService.recoverModel(projectUrl, modelUrl);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Recover model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<ModelInfoVo>> getModelInfo(
            String projectUrl, String modelUrl, String versionUrl
    ) {
        ModelInfoVo modelInfo = modelService.getModelInfo(
                ModelQuery.builder()
                        .projectUrl(projectUrl)
                        .modelUrl(modelUrl)
                        .modelVersionUrl(versionUrl)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(modelInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<Map<String, List<FileNode>>>> getModelDiff(
            String projectUrl, String modelUrl, String baseVersion, String compareVersion
    ) {

        return ResponseEntity.ok(Code.success.asResponse(
                modelService.getModelDiff(projectUrl, modelUrl, baseVersion, compareVersion)));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ModelVersionVo>>> listModelVersion(
            String projectUrl,
            String modelUrl,
            String name,
            Integer pageNum,
            Integer pageSize
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

    @Override
    public ResponseEntity<ResponseMessage<String>> shareModelVersion(
            String projectUrl, String modelUrl, String versionUrl, Boolean shared
    ) {
        modelService.shareModelVersion(projectUrl, modelUrl, versionUrl, shared);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ModelViewVo>>> listModelTree(String projectUrl, DataRange range) {
        List<ModelViewVo> list;
        switch (range) {
            case all:
                list = modelService.listModelVersionView(projectUrl, true, true);
                break;
            case shared:
                list = modelService.listModelVersionView(projectUrl, true, false);
                break;
            case project:
                list = modelService.listModelVersionView(projectUrl, false, true);
                break;
            default:
                list = List.of();
        }
        return ResponseEntity.ok(Code.success.asResponse(list));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyModel(
            String projectUrl, String modelUrl, String versionUrl, ModelUpdateRequest request
    ) {
        Boolean res = modelService.modifyModelVersion(
                projectUrl,
                modelUrl,
                versionUrl,
                ModelVersion.builder()
                        .tag(request.getTag())
                        .builtInRuntime(request.getBuiltInRuntime())
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

    @Override
    public ResponseEntity<ResponseMessage<String>> addModelVersionTag(
            String projectUrl,
            String modelUrl,
            String versionUrl,
            ModelTagRequest modelTagRequest
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

    @Override
    public ResponseEntity<ResponseMessage<List<String>>> listModelVersionTags(
            String projectUrl,
            String modelUrl,
            String versionUrl
    ) {
        var tags = modelService.listModelVersionTags(projectUrl, modelUrl, versionUrl);
        return ResponseEntity.ok(Code.success.asResponse(tags));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModelVersionTag(
            String projectUrl,
            String modelUrl,
            String versionUrl,
            String tag
    ) {
        modelService.deleteModelVersionTag(projectUrl, modelUrl, versionUrl, tag);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<Long>> getModelVersionTag(String projectUrl, String modelUrl, String tag) {
        var entity = modelService.getModelVersionTag(projectUrl, modelUrl, tag);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Code.success.asResponse(entity.getVersionId()));
    }


    @Override
    public ResponseEntity<ResponseMessage<InitUploadBlobResult>> initUploadBlob(
            InitUploadBlobRequest initUploadBlobRequest
    ) {
        var result = this.modelService.initUploadBlob(initUploadBlobRequest);
        return ResponseEntity.ok(Code.success.asResponse(result));
    }

    @Override
    public ResponseEntity<ResponseMessage<CompleteUploadBlobResult>> completeUploadBlob(String blobId) {
        var result = this.modelService.completeUploadBlob(blobId);
        return ResponseEntity.ok(Code.success.asResponse(CompleteUploadBlobResult.builder().blobId(result).build()));
    }

    @Override
    public void createModelVersion(
            String project, String modelName, String version,
            CreateModelVersionRequest createModelVersionRequest
    ) {
        this.modelService.createModelVersion(project, modelName, version, createModelVersionRequest);
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> getModelMetaBlob(
            String project, String model, String version, String blobId
    ) {
        var root = this.modelService.getModelMetaBlob(project, model, version, blobId);
        try {
            return ResponseEntity.ok(Code.success.asResponse(JsonFormat.printer().print(root)));
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.SYSTEM, "failed to print protobuf", e);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<ListFilesResult>> listFiles(
            String project, String model, String version, String path
    ) {
        var result = this.modelService.listFiles(project, model, version, path);
        return ResponseEntity.ok(Code.success.asResponse(result));
    }

    @Override
    public ResponseEntity<InputStreamResource> getFileData(String project, String model, String version, String path) {
        var result = this.modelService.getFileData(project, model, version, path);
        return ResponseEntity.ok(new InputStreamResource(result) {
            @Override
            public long contentLength() {
                return result.getSize();
            }
        });
    }

    @Override
    public ResponseEntity<?> headModel(String projectUrl, String modelUrl, String versionUrl) {
        try {
            modelService.query(projectUrl, modelUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head model result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }
}
