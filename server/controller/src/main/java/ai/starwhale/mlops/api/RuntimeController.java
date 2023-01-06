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
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeRevertRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeTagRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("${sw.controller.api-prefix}")
public class RuntimeController implements RuntimeApi {

    private final RuntimeService runtimeService;

    public RuntimeController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<RuntimeVo>>> listRuntime(String projectUrl,
            String name, String owner, Integer pageNum, Integer pageSize) {
        PageInfo<RuntimeVo> pageInfo = runtimeService.listRuntime(
                RuntimeQuery.builder()
                        .projectUrl(projectUrl)
                        .namePrefix(name)
                        .owner(owner)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> revertRuntimeVersion(String projectUrl,
            String runtimeUrl, RuntimeRevertRequest revertRequest) {
        Boolean res = runtimeService.revertVersionTo(projectUrl, runtimeUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Revert runtime version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteRuntime(String projectUrl,
            String runtimeUrl) {
        Boolean res = runtimeService.deleteRuntime(
                RuntimeQuery.builder()
                        .projectUrl(projectUrl)
                        .runtimeUrl(runtimeUrl)
                        .build());

        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Delete runtime failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverRuntime(String projectUrl,
            String runtimeUrl) {
        Boolean res = runtimeService.recoverRuntime(projectUrl, runtimeUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Recover runtime failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));

    }

    @Override
    public ResponseEntity<ResponseMessage<RuntimeInfoVo>> getRuntimeInfo(String projectUrl,
            String runtimeUrl, String versionUrl) {
        RuntimeInfoVo runtimeInfo = runtimeService.getRuntimeInfo(
                RuntimeQuery.builder()
                        .projectUrl(projectUrl)
                        .runtimeUrl(runtimeUrl)
                        .runtimeVersionUrl(versionUrl)
                        .build());

        return ResponseEntity.ok(Code.success.asResponse(runtimeInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyRuntime(String projectUrl,
            String runtimeUrl, String runtimeVersionUrl, RuntimeTagRequest tagRequest) {
        Boolean res = runtimeService.modifyRuntimeVersion(projectUrl, runtimeUrl, runtimeVersionUrl,
                RuntimeVersion.builder()
                        .versionTag(tagRequest.getTag()).build());

        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Modify runtime failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> manageRuntimeTag(String projectUrl,
            String runtimeUrl, String versionUrl, RuntimeTagRequest tagRequest) {
        TagAction ta;
        try {
            ta = TagAction.of(tagRequest.getAction(), tagRequest.getTag());
        } catch (IllegalArgumentException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.RUNTIME,
                            String.format("Unknown tag action %s ", tagRequest.getAction()),
                            e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = runtimeService.manageVersionTag(projectUrl, runtimeUrl, versionUrl, ta);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Update runtime tag failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<RuntimeVersionVo>>> listRuntimeVersion(
            String projectUrl, String runtimeUrl, String versionName, String versionTag,
            Integer pageNum, Integer pageSize) {
        PageInfo<RuntimeVersionVo> pageInfo = runtimeService.listRuntimeVersionHistory(
                RuntimeVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .runtimeUrl(runtimeUrl)
                        .versionName(versionName)
                        .versionTag(versionTag)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> upload(String projectUrl, String runtimeUrl, String versionUrl,
            MultipartFile file, ClientRuntimeRequest uploadRequest) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setRuntime(runtimeUrl + ":" + versionUrl);
        runtimeService.upload(file, uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public void pull(String projectUrl, String runtimeUrl, String versionUrl,
            HttpServletResponse httpResponse) {
        runtimeService.pull(projectUrl, runtimeUrl, versionUrl, httpResponse);
    }

    @Override
    public ResponseEntity<?> headRuntime(String projectUrl, String runtimeUrl, String versionUrl) {
        try {
            runtimeService.query(projectUrl, runtimeUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head runtime result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }
}
