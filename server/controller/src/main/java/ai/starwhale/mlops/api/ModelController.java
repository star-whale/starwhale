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
import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelTagRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUpdateRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUploadRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelViewVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.model.RevertModelVersionRequest;
import ai.starwhale.mlops.api.protocol.storage.FileDesc;
import ai.starwhale.mlops.api.protocol.storage.FileNode;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ResponseMessage<PageInfo<ModelVo>>> listModel(String projectUrl, String versionId,
            String name, String owner, Integer pageNum, Integer pageSize) {
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
                            .build());
        }
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> revertModelVersion(String projectUrl, String modelUrl,
            RevertModelVersionRequest revertRequest) {
        Boolean res = modelService.revertVersionTo(projectUrl, modelUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Revert model version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
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
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Delete model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverModel(String projectUrl, String modelUrl) {
        Boolean res = modelService.recoverModel(projectUrl, modelUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Recover model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<ModelInfoVo>> getModelInfo(
            String projectUrl, String modelUrl, String versionUrl) {
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
            String projectUrl, String modelUrl, String baseVersion, String compareVersion) {

        return ResponseEntity.ok(Code.success.asResponse(
                modelService.getModelDiff(projectUrl, modelUrl, baseVersion, compareVersion)));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ModelVersionVo>>> listModelVersion(String projectUrl,
            String modelUrl, String name, String tag, Integer pageNum, Integer pageSize) {
        PageInfo<ModelVersionVo> pageInfo = modelService.listModelVersionHistory(
                ModelVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .modelUrl(modelUrl)
                        .versionName(name)
                        .versionTag(tag)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> shareModelVersion(
            String projectUrl, String modelUrl, String versionUrl, Boolean shared) {
        modelService.shareModelVersion(projectUrl, modelUrl, versionUrl, shared);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ModelViewVo>>> listModelTree(String projectUrl) {
        return ResponseEntity.ok(Code.success.asResponse(modelService.listModelVersionView(projectUrl)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyModel(
            String projectUrl, String modelUrl, String versionUrl, ModelUpdateRequest request) {
        Boolean res = modelService.modifyModelVersion(projectUrl, modelUrl, versionUrl,
                ModelVersion.builder()
                    .tag(request.getTag())
                    .builtInRuntime(request.getBuiltInRuntime())
                    .build());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Update model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> manageModelTag(String projectUrl,
            String modelUrl, String versionUrl, ModelTagRequest modelTagRequest) {
        TagAction ta;
        try {
            ta = TagAction.of(modelTagRequest.getAction(), modelTagRequest.getTag());
        } catch (IllegalArgumentException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL,
                            String.format("Unknown tag action %s ", modelTagRequest.getAction()),
                            e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = modelService.manageVersionTag(projectUrl, modelUrl, versionUrl, ta);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Update model tag failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<Object>> upload(
            String projectUrl, String modelUrl, String versionUrl,
            MultipartFile file, ModelUploadRequest uploadRequest) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setSwmp(modelUrl + ":" + versionUrl);
        FileDesc fileDesc = uploadRequest.getDesc();
        String signature = uploadRequest.getSignature();
        Long uploadId = uploadRequest.getUploadId();
        switch (uploadRequest.getPhase()) {
            case MANIFEST:
                return ResponseEntity.ok(Code.success.asResponse(
                        modelService.uploadManifest(file, uploadRequest)));
            case BLOB:
                switch (fileDesc) {
                    case MODEL:
                        modelService.uploadModel(uploadId, signature, file, uploadRequest);
                        break;
                    case SRC_TAR:
                        modelService.uploadSrc(uploadId, file, uploadRequest);
                        break;
                    default:
                        throw new StarwhaleApiException(
                                new SwValidationException(ValidSubject.MODEL, "don't support fileType" + fileDesc),
                                HttpStatus.BAD_REQUEST);
                }
                break;
            case CANCEL:
                // TODO need use a tmp record otherwise the origin record will be removed when use force
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.MODEL, "don't support cancel"),
                        HttpStatus.BAD_REQUEST);
            case END:
                modelService.end(uploadId);
                break;
            default:
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.MODEL, "unknown phase " + uploadRequest.getPhase()),
                        HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public void pull(FileDesc fileDesc, String name, String path, String signature,
                     String projectUrl, String modelUrl, String versionUrl,
                     HttpServletResponse httpResponse) {
        modelService.pull(fileDesc, name, path, signature, projectUrl, modelUrl, versionUrl, httpResponse);
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
