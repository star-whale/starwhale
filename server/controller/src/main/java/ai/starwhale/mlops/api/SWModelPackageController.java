/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swmp.ClientSWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.RevertSWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.swmp.SWMPFile;
import ai.starwhale.mlops.domain.swmp.SWMPObject;
import ai.starwhale.mlops.domain.swmp.SWMPObject.Version;
import ai.starwhale.mlops.domain.swmp.SWModelPackageService;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.ApiOperationException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.annotation.Resource;
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
    private UserService userService;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(String projectId, String versionId,
        String modelName, Integer pageNum, Integer pageSize) {
        PageInfo<SWModelPackageVO> pageInfo;
        if(StringUtils.hasText(versionId)) {
            List<SWModelPackageVO> voList = swmpService.findModelByVersionId(
                List.of(versionId.split("[,;]")));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = swmpService.listSWMP(
                SWMPObject.builder().projectId(projectId).name(modelName).build(),
                PageParams.builder().pageNum(pageNum).pageSize(pageSize).build());
        }
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> revertModelVersion(String projectId,String modelId,
        RevertSWMPVersionRequest revertRequest) {
        SWMPObject swmp = SWMPObject.builder()
            .id(modelId)
            .projectId(projectId)
            .latestVersion(Version.builder().id(revertRequest.getVersionId()).build())
            .build();
        Boolean res = swmpService.revertVersionTo(swmp);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Revert swmp version failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModelById(String projectId,String modelId) {
        Boolean res = swmpService.deleteSWMP(
            SWMPObject.builder().projectId(projectId).id(modelId).build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete swmp failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SWModelPackageInfoVO>> getModelInfo(String projectId,String modelId) {
        SWModelPackageInfoVO swmpInfo = swmpService.getSWMPInfo(
            SWMPObject.builder().projectId(projectId).id(modelId).build());
        return ResponseEntity.ok(Code.success.asResponse(swmpInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVersionVO>>> listModelVersion(String projectId,
        String modelId, String modelVersionName, Integer pageNum, Integer pageSize) {
        PageInfo<SWModelPackageVersionVO> pageInfo = swmpService.listSWMPVersionHistory(
            SWMPObject.builder()
                .projectId(projectId)
                .id(modelId)
                .latestVersion(Version.builder().name(modelVersionName).build())
                .build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createModelVersion(String projectId, String modelId,
        MultipartFile zipFile, SWMPVersionRequest request) {
        User user = userService.currentUserDetail();
        String versionId = createVersion(projectId, modelId, zipFile, request.getImportPath(), user.getId());
        log.info("Create swmp version successfully, id = {}", versionId);
        return ResponseEntity.ok(Code.success
            .asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyModel(String projectId, String modelId, String versionId,
        String tag) {
        Boolean res = swmpService.modifySWMPVersion(
            Version.builder().id(versionId).tag(tag).build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update swmp failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createModel(String projectId, MultipartFile zipFile, SWMPRequest swmpRequest) {
        User user = userService.currentUserDetail();
        String modelId = swmpService.addSWMP(
            SWMPObject.builder().projectId(projectId).name(swmpRequest.getModelName()).ownerId(user.getId())
                .build());
        String versionId = createVersion(projectId, modelId, zipFile, swmpRequest.getImportPath(), user.getId());
        log.info("Create swmp successfully, version id = {}", versionId);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> upload(MultipartFile dsFile,
        ClientSWMPRequest uploadRequest) {
        swmpService.upload(dsFile,uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Override
    public byte[] pull(ClientSWMPRequest pullRequest) {
        return swmpService.pull(pullRequest);
    }

    private String createVersion(String projectId, String modelId, MultipartFile zipFile, String importPath, String userId) {
        String path = importPath;
        String meta = "";
        if (zipFile != null) {
            // upload file
            SWMPFile swmpFile = new SWMPFile(projectId, modelId);
            String fileName = swmpFile.generateZipFileName();
            File dest = new File(swmpFile.getZipFilePath(), fileName);
            try {
                zipFile.transferTo(dest);
            } catch (IOException e) {
                throw new ApiOperationException("Model File upload error.");
            }
            path = dest.getPath();
            meta = swmpFile.meta();
        }
        SWMPObject swmp = SWMPObject.builder()
            .id(modelId)
            .latestVersion(Version.builder()
                .storagePath(path)
                .meta(meta)
                .name(RandomUtil.randomHexString(8))
                .tag("")
                .ownerId(userId)
                .build())
            .build();
        return swmpService.addVersion(swmp);
    }
}
