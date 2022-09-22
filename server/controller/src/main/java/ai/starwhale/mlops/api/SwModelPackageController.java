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
import ai.starwhale.mlops.api.protocol.swmp.ClientSwmpRequest;
import ai.starwhale.mlops.api.protocol.swmp.RevertSwmpVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageInfoVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVersionVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVo;
import ai.starwhale.mlops.api.protocol.swmp.SwmpTagRequest;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.swmp.SwModelPackageService;
import ai.starwhale.mlops.domain.swmp.bo.SwmpQuery;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersion;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersionQuery;
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
public class SwModelPackageController implements SwModelPackageApi {

    private final SwModelPackageService swmpService;
    private final IdConvertor idConvertor;

    public SwModelPackageController(SwModelPackageService swmpService, IdConvertor idConvertor) {
        this.swmpService = swmpService;
        this.idConvertor = idConvertor;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SwModelPackageVo>>> listModel(String projectUrl, String versionId,
            String modelName, Integer pageNum, Integer pageSize) {
        PageInfo<SwModelPackageVo> pageInfo;
        if (StringUtils.hasText(versionId)) {
            List<SwModelPackageVo> voList = swmpService
                    .findModelByVersionId(Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                            Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = swmpService.listSwmp(
                    SwmpQuery.builder()
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
    public ResponseEntity<ResponseMessage<String>> revertModelVersion(String projectUrl, String swmpUrl,
            RevertSwmpVersionRequest revertRequest) {
        Boolean res = swmpService.revertVersionTo(projectUrl, swmpUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Revert swmp version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModel(String projectUrl, String modelUrl) {
        Boolean res = swmpService.deleteSwmp(SwmpQuery.builder()
                .projectUrl(projectUrl)
                .swmpUrl(modelUrl)
                .build());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Delete swmp failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverModel(String projectUrl, String modelUrl) {
        Boolean res = swmpService.recoverSwmp(projectUrl, modelUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Recover model failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SwModelPackageInfoVo>> getModelInfo(String projectUrl, String modelUrl,
            String versionUrl) {
        SwModelPackageInfoVo swmpInfo = swmpService.getSwmpInfo(
                SwmpQuery.builder()
                        .projectUrl(projectUrl)
                        .swmpUrl(modelUrl)
                        .swmpVersionUrl(versionUrl)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(swmpInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SwModelPackageVersionVo>>> listModelVersion(String projectUrl,
            String modelUrl, String name, String tag, Integer pageNum, Integer pageSize) {
        PageInfo<SwModelPackageVersionVo> pageInfo = swmpService.listSwmpVersionHistory(
                SwmpVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .swmpUrl(modelUrl)
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
            SwmpTagRequest swmpTagRequest) {
        Boolean res = swmpService.modifySwmpVersion(projectUrl, modelUrl, versionUrl,
                SwmpVersion.builder().tag(swmpTagRequest.getTag()).build());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Update swmp failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> manageModelTag(String projectUrl,
            String modelUrl, String versionUrl, SwmpTagRequest swmpTagRequest) {
        TagAction ta;
        try {
            ta = TagAction.of(swmpTagRequest.getAction(), swmpTagRequest.getTag());
        } catch (IllegalArgumentException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP).tip(
                    String.format("Unknown tag action %s ", swmpTagRequest.getAction())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = swmpService.manageVersionTag(projectUrl, modelUrl, versionUrl, ta);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Update model tag failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> upload(String projectUrl, String modelUrl, String versionUrl,
            MultipartFile dsFile, ClientSwmpRequest uploadRequest) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setSwmp(modelUrl + ":" + versionUrl);
        swmpService.upload(dsFile, uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public void pull(String projectUrl, String modelUrl, String versionUrl, HttpServletResponse httpResponse) {
        swmpService.pull(projectUrl, modelUrl, versionUrl, httpResponse);
    }

    @Override
    public ResponseEntity<?> headModel(String projectUrl, String modelUrl, String versionUrl) {
        try {
            swmpService.query(projectUrl, modelUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head model result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }
}
