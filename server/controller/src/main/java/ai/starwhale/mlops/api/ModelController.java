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
import ai.starwhale.mlops.api.protocol.model.ClientModelRequest;
import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelTagRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.model.RevertModelVersionRequest;
import ai.starwhale.mlops.common.IdConvertor;
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
@RequestMapping("${sw.controller.apiPrefix}")
public class ModelController implements ModelApi {

    private final ModelService modelService;
    private final IdConvertor idConvertor;

    public ModelController(ModelService modelService, IdConvertor idConvertor) {
        this.modelService = modelService;
        this.idConvertor = idConvertor;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ModelVo>>> listModel(String projectUrl, String versionId,
            String modelName, Integer pageNum, Integer pageSize) {
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
                            .namePrefix(modelName)
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
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Revert model version failed."),
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
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Delete model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverModel(String projectUrl, String modelUrl) {
        Boolean res = modelService.recoverModel(projectUrl, modelUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Recover model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<ModelInfoVo>> getModelInfo(String projectUrl, String modelUrl,
            String versionUrl) {
        ModelInfoVo modelInfo = modelService.getModelInfo(
                ModelQuery.builder()
                        .projectUrl(projectUrl)
                        .modelUrl(modelUrl)
                        .modelVersionUrl(versionUrl)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(modelInfo));
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
    public ResponseEntity<ResponseMessage<String>> modifyModel(String projectUrl, String modelUrl, String versionUrl,
            ModelTagRequest modelTagRequest) {
        Boolean res = modelService.modifyModelVersion(projectUrl, modelUrl, versionUrl,
                ModelVersion.builder().tag(modelTagRequest.getTag()).build());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Update model failed."),
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
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL).tip(
                    String.format("Unknown tag action %s ", modelTagRequest.getAction())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = modelService.manageVersionTag(projectUrl, modelUrl, versionUrl, ta);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Update model tag failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> upload(String projectUrl, String modelUrl, String versionUrl,
            MultipartFile dsFile, ClientModelRequest uploadRequest) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setSwmp(modelUrl + ":" + versionUrl);
        modelService.upload(dsFile, uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public void pull(String projectUrl, String modelUrl, String versionUrl, HttpServletResponse httpResponse) {
        modelService.pull(projectUrl, modelUrl, versionUrl, httpResponse);
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
