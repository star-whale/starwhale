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
import ai.starwhale.mlops.api.protocol.swmp.ClientSWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.RevertSWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPTagRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.swmp.SWModelPackageService;
import ai.starwhale.mlops.domain.swmp.bo.SWMPQuery;
import ai.starwhale.mlops.domain.swmp.bo.SWMPVersion;
import ai.starwhale.mlops.domain.swmp.bo.SWMPVersionQuery;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
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
public class SWModelPackageController implements SWModelPackageApi{

    @Resource
    private SWModelPackageService swmpService;

    @Resource
    private IDConvertor idConvertor;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(String projectUrl, String versionId,
        String modelName, Integer pageNum, Integer pageSize) {
        PageInfo<SWModelPackageVO> pageInfo;
        if(StringUtils.hasText(versionId)) {
            List<SWModelPackageVO> voList = swmpService
                .findModelByVersionId(Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                    Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = swmpService.listSWMP(
                SWMPQuery.builder()
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
        RevertSWMPVersionRequest revertRequest) {
        Boolean res = swmpService.revertVersionTo(projectUrl, swmpUrl, revertRequest.getVersion());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Revert swmp version failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModel(String projectUrl, String modelUrl) {
        Boolean res = swmpService.deleteSWMP(SWMPQuery.builder()
            .projectUrl(projectUrl)
            .swmpUrl(modelUrl)
            .build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete swmp failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverModel(String projectUrl, String modelUrl) {
        Boolean res = swmpService.recoverSWMP(projectUrl, modelUrl);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Recover model failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SWModelPackageInfoVO>> getModelInfo(String projectUrl, String modelUrl, String versionUrl) {
        SWModelPackageInfoVO swmpInfo = swmpService.getSWMPInfo(
           SWMPQuery.builder()
               .projectUrl(projectUrl)
               .swmpUrl(modelUrl)
               .swmpVersionUrl(versionUrl)
               .build());
        return ResponseEntity.ok(Code.success.asResponse(swmpInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVersionVO>>> listModelVersion(String projectUrl,
        String modelUrl, String name, String tag, Integer pageNum, Integer pageSize) {
        PageInfo<SWModelPackageVersionVO> pageInfo = swmpService.listSWMPVersionHistory(
            SWMPVersionQuery.builder()
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
        SWMPTagRequest swmpTagRequest) {
        Boolean res = swmpService.modifySWMPVersion(projectUrl, modelUrl, versionUrl,
            SWMPVersion.builder().tag(swmpTagRequest.getTag()).build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update swmp failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> manageModelTag(String projectUrl,
        String modelUrl, String versionUrl, SWMPTagRequest swmpTagRequest) {
        TagAction ta;
        try {
            ta = TagAction.of(swmpTagRequest.getAction(), swmpTagRequest.getTag());
        } catch (IllegalArgumentException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP).tip(String.format("Unknown tag action %s ", swmpTagRequest.getAction())),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = swmpService.manageVersionTag(projectUrl, modelUrl, versionUrl, ta);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update model tag failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> upload(MultipartFile dsFile,
        ClientSWMPRequest uploadRequest) {
        swmpService.upload(dsFile,uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public void pull(ClientSWMPRequest pullRequest, HttpServletResponse httpResponse) {
        swmpService.pull(pullRequest,httpResponse);
    }

    @Override
    public ResponseEntity<ResponseMessage<List<SWModelPackageInfoVO>>> listModel(String project,
        String name) {
        List<SWModelPackageInfoVO> swModelPackageInfoVOS = swmpService.listSWMPInfo(project, name);
        return ResponseEntity.ok(Code.success.asResponse(swModelPackageInfoVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> uploadModel(MultipartFile file,
        ClientSWMPRequest uploadRequest) {
        return upload(file,uploadRequest);
    }

    @Override
    public void pullModel(ClientSWMPRequest pullRequest, HttpServletResponse httpResponse) {
        pull(pullRequest,httpResponse);
    }

    @Override
    public ResponseEntity<String> headModel(ClientSWMPRequest queryRequest) {
        return ResponseEntity.ok(swmpService.query(queryRequest));
    }
}
