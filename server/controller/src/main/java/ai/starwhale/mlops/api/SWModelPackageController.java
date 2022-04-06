/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swmp.RevertSWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWMPVersionRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.swmp.SWMPFile;
import ai.starwhale.mlops.domain.swmp.SWMPObject;
import ai.starwhale.mlops.domain.swmp.SWMPObject.Version;
import ai.starwhale.mlops.domain.swmp.SWModelPackageService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.ApiOperationException;
import cn.hutool.core.lang.Assert;
import com.github.pagehelper.PageInfo;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SWModelPackageController implements SWModelPackageApi{

    @Resource
    private SWModelPackageService swmpService;

    @Resource
    private UserService userService;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(String projectId, String versionId,
        String modelName, Integer pageNum, Integer pageSize) {
        List<SWModelPackageVO> voList;
        if(StringUtils.hasText(versionId)) {
            voList = swmpService.findModelByVersionId(List.of(versionId.split("[,;]")));
        } else {
            voList = swmpService.listSWMP(
                SWMPObject.builder().projectId(projectId).name(modelName).build(),
                PageParams.builder().pageNum(pageNum).pageSize(pageSize).build());
        }
        PageInfo<SWModelPackageVO> pageInfo = new PageInfo<>(voList);
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
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModelById(String projectId,String modelId) {
        Boolean res = swmpService.deleteSWMP(
            SWMPObject.builder().projectId(projectId).id(modelId).build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
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
        List<SWModelPackageVersionVO> voList = swmpService.listSWMPVersionHistory(
            SWMPObject.builder()
                .projectId(projectId)
                .id(modelId)
                .latestVersion(Version.builder().name(modelVersionName).build())
                .build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        PageInfo<SWModelPackageVersionVO> pageInfo = new PageInfo<>(voList);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createModelVersion(String projectId, String modelId,
        MultipartFile zipFile, SWMPVersionRequest request) {
        UserVO user = userService.currentUser();
        String versionId = createVersion(projectId, modelId, zipFile, request.getImportPath(), user.getId());
        return ResponseEntity.ok(Code.success
            .asResponse(String.valueOf(Optional.of(versionId).orElseThrow(ApiOperationException::new))));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyModel(String projectId, String modelId, String versionId,
        String tag) {
        Boolean res = swmpService.modifySWMPVersion(
            Version.builder().id(versionId).tag(tag).build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createModel(String projectId, MultipartFile zipFile, SWMPRequest swmpRequest) {
        UserVO user = userService.currentUser();
        String modelId = swmpService.addSWMP(
            SWMPObject.builder().projectId(projectId).name(swmpRequest.getModelName()).ownerId(user.getId())
                .build());
        String versionId = createVersion(projectId, modelId, zipFile, swmpRequest.getImportPath(), user.getId());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(Optional.of(versionId).orElseThrow(ApiOperationException::new))));
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
