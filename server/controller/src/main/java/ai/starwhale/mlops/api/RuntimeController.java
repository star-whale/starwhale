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
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVO;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeRevertRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVO;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.runtime.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.RuntimeVersionQuery;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class RuntimeController implements RuntimeApi {

    @Resource
    private RuntimeService runtimeService;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<RuntimeVO>>> listRuntime(String projectUrl,
        String runtimeName, Integer pageNum, Integer pageSize) {
        PageInfo<RuntimeVO> pageInfo = runtimeService.listRuntime(
            RuntimeQuery.builder()
                .projectUrl(projectUrl)
                .namePrefix(runtimeName)
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
        Boolean res = runtimeService.revertVersionTo(runtimeUrl, revertRequest.getVersionUrl());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Revert runtime version failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteRuntime(String projectUrl,
        String runtimeUrl) {
        Boolean res = runtimeService.deleteRuntime(
            RuntimeQuery.builder().runtimeUrl(runtimeUrl).build());

        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete runtime failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverRuntime(String projectUrl,
        String runtimeUrl) {
        Boolean res = runtimeService.recoverRuntime(projectUrl, runtimeUrl);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Recover runtime failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));

    }

    @Override
    public ResponseEntity<ResponseMessage<RuntimeInfoVO>> getRuntimeInfo(String projectUrl,
        String runtimeUrl, String runtimeVersionUrl) {
        RuntimeInfoVO runtimeInfo = runtimeService.getRuntimeInfo(
            RuntimeQuery.builder()
                .runtimeUrl(runtimeUrl)
                .runtimeVersionUrl(runtimeVersionUrl)
                .build());

        return ResponseEntity.ok(Code.success.asResponse(runtimeInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyRuntime(String projectUrl,
        String runtimeUrl, String runtimeVersionUrl, String tag) {
        Boolean res = runtimeService.modifyRuntimeVersion(runtimeUrl, runtimeVersionUrl,
            RuntimeVersion.builder()
                .versionTag(tag).build());

        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Modify runtime failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<RuntimeInfoVO>>> listRuntimeInfo(String project,
        String name) {
        List<RuntimeInfoVO> runtimeInfoVOS = runtimeService.listRuntimeInfo(project, name);
        return ResponseEntity.ok(Code.success.asResponse(runtimeInfoVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<RuntimeVersionVO>>> listRuntimeVersion(
        String projectUrl, String runtimeUrl, String vName, String vTag,
        Integer pageNum, Integer pageSize) {
        PageInfo<RuntimeVersionVO> pageInfo = runtimeService.listRuntimeVersionHistory(
            RuntimeVersionQuery.builder()
                .projectUrl(projectUrl)
                .runtimeUrl(runtimeUrl)
                .versionName(vName)
                .versionTag(vTag)
                .build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> upload(MultipartFile file, ClientRuntimeRequest uploadRequest) {
        runtimeService.upload(file, uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public void pull(ClientRuntimeRequest pullRequest, HttpServletResponse httpResponse) {
        runtimeService.pull(pullRequest, httpResponse);
    }

    @Override
    public ResponseEntity<String> headRuntime(ClientRuntimeRequest queryRequest) {
        return ResponseEntity.ok(runtimeService.query(queryRequest));
    }
}
