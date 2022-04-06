/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.api.protocol.swds.RevertSWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSVersionRequest;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.swds.SWDSFile;
import ai.starwhale.mlops.domain.swds.SWDSObject;
import ai.starwhale.mlops.domain.swds.SWDSObject.Version;
import ai.starwhale.mlops.domain.swds.SWDatasetService;
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
public class DatasetController implements DatasetApi{

    @Resource
    private SWDatasetService swDatasetService;

    @Resource
    private UserService userService;

    @Override
    public ResponseEntity<ResponseMessage<String>> revertDatasetVersion(String projectId,
        String datasetId, RevertSWDSRequest revertRequest) {
        SWDSObject swmp = SWDSObject.builder()
            .id(datasetId)
            .projectId(projectId)
            .latestVersion(Version.builder().id(revertRequest.getVersionId()).build())
            .build();
        Boolean res = swDatasetService.revertVersionTo(swmp);
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDatasetById(String projectId,
        String datasetId) {
        Boolean res = swDatasetService.deleteSWDS(
            SWDSObject.builder().projectId(projectId).id(datasetId).build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<DatasetVersionVO>> getDatasetInfo(String projectId,
        String datasetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(
        String projectId, String datasetId, String dsVersionName, Integer pageNum, Integer pageSize) {
        List<DatasetVersionVO> voList = swDatasetService.listDatasetVersionHistory(
            SWDSObject.builder()
                .projectId(projectId)
                .id(datasetId)
                .latestVersion(Version.builder().name(dsVersionName).build())
                .build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        PageInfo<DatasetVersionVO> pageInfo = new PageInfo<>(voList);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createDatasetVersion(String projectId, String datasetId,
        MultipartFile zipFile, SWDSVersionRequest swdsVersionRequest) {
        UserVO user = userService.currentUser();
        String versionId = createVersion(projectId, datasetId, zipFile, swdsVersionRequest.getImportPath(), user.getId());
        return ResponseEntity.ok(Code.success
            .asResponse(String.valueOf(Optional.of(versionId).orElseThrow(ApiOperationException::new))));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyDatasetVersionInfo(String projectId, String datasetId,
        String versionId, String tag) {
        Boolean res = swDatasetService.modifySWDSVersion(
            Version.builder().id(versionId).tag(tag).build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(String projectId, String versionId,
        Integer pageNum, Integer pageSize) {
        List<DatasetVO> voList;
        if(StringUtils.hasText(versionId)) {
            voList = swDatasetService.findDatasetsByVersionIds(List.of(versionId.split("[,;]")));
        } else {
            voList = swDatasetService.listSWDataset(
                SWDSObject.builder().projectId(projectId).build(),
                PageParams.builder().pageNum(pageNum).pageSize(pageSize).build());
        }
        PageInfo<DatasetVO> pageInfo = new PageInfo<>(voList);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createDataset(String projectId,
        String datasetName, MultipartFile zipFile, SWDSRequest swdsRequest) {
        UserVO user = userService.currentUser();
        String modelId = swDatasetService.addDataset(
            SWDSObject.builder().projectId(projectId).name(datasetName).ownerId(user.getId())
                .build());
        String versionId = createVersion(projectId, modelId, zipFile, swdsRequest.getImportPath(), user.getId());
        return ResponseEntity.ok(Code.success
            .asResponse(String.valueOf(Optional.of(versionId).orElseThrow(ApiOperationException::new))));
    }

    private String createVersion(String projectId, String datasetId, MultipartFile zipFile, String importPath, String userId) {
        String path = importPath;
        String meta = "";
        if (zipFile != null) {
            // upload file
            SWDSFile swdsFile = new SWDSFile(projectId, datasetId);
            File dest = new File(swdsFile.getZipFilePath(), swdsFile.generateZipFileName());
            try {
                zipFile.transferTo(dest);
            } catch (IOException e) {
                throw new ApiOperationException("Dataset File upload error.");
            }
            path = dest.getPath();
            meta = swdsFile.meta();
        }
        SWDSObject swmp = SWDSObject.builder()
            .id(datasetId)
            .latestVersion(Version.builder()
                .storagePath(path)
                .meta(meta)
                .name(RandomUtil.randomHexString(8))
                .tag("")
                .ownerId(userId)
                .build())
            .build();
        return swDatasetService.addVersion(swmp);
    }


}
