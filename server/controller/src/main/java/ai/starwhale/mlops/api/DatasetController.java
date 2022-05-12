/**
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
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.api.protocol.swds.RevertSWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSVersionRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDatasetInfoVO;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadResult;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.swds.SWDSFile;
import ai.starwhale.mlops.domain.swds.SWDSObject;
import ai.starwhale.mlops.domain.swds.SWDSVersion;
import ai.starwhale.mlops.domain.swds.SWDatasetService;
import ai.starwhale.mlops.domain.swds.upload.SwdsUploader;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.ApiOperationException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.lang.Assert;
import com.github.pagehelper.PageInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
@Slf4j
public class DatasetController implements DatasetApi{

    @Resource
    private SWDatasetService swDatasetService;

    @Resource
    private UserService userService;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private SwdsUploader swdsUploader;

    @Override
    public ResponseEntity<ResponseMessage<String>> revertDatasetVersion(String projectId,
        String datasetId, RevertSWDSRequest revertRequest) {
        SWDSObject swmp = SWDSObject.builder()
            .id(idConvertor.revert(datasetId))
            .projectId(idConvertor.revert(projectId))
            .currentVersion(SWDSVersion.builder().id(idConvertor.revert(revertRequest.getVersionId())).build())
            .build();
        Boolean res = swDatasetService.revertVersionTo(swmp);
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDatasetById(String projectId,
        String datasetId) {
        Boolean res = swDatasetService.deleteSWDS(
            SWDSObject.builder()
                .projectId(idConvertor.revert(projectId))
                .id(idConvertor.revert(datasetId))
                .build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SWDatasetInfoVO>> getDatasetInfo(String projectId,
        String datasetId) {
        SWDatasetInfoVO swdsInfo = swDatasetService.getSWDSInfo(
            SWDSObject.builder()
                .id(idConvertor.revert(datasetId))
                .projectId(idConvertor.revert(projectId))
                .build());

        return ResponseEntity.ok(Code.success.asResponse(swdsInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(
        String projectId, String datasetId, String vName, String tag, Integer pageNum, Integer pageSize) {
        PageInfo<DatasetVersionVO> pageInfo = swDatasetService.listDatasetVersionHistory(
            SWDSObject.builder()
                .projectId(idConvertor.revert(projectId))
                .id(idConvertor.revert(datasetId))
                .build(),
            SWDSVersion.builder()
                .name(vName)
                .tag(tag)
                .build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createDatasetVersion(String projectId, String datasetId,
        MultipartFile zipFile, SWDSVersionRequest swdsVersionRequest) {
        User user = userService.currentUserDetail();
        Long versionId = createVersion(projectId, idConvertor.revert(datasetId) , zipFile, swdsVersionRequest.getImportPath(), user.getId());
        return ResponseEntity.ok(Code.success
            .asResponse(String.valueOf(Optional.of(versionId).orElseThrow(ApiOperationException::new))));
    }

    @Override
    public ResponseEntity<ResponseMessage<UploadResult>> uploadDS(String uploadId,
        MultipartFile dsFile, UploadRequest uploadRequest) {
        switch (uploadRequest.getPhase()){
            case MANIFEST:
                String text;
                try (final InputStream inputStream = dsFile.getInputStream()){
                    text = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    log.error("read manifest file failed",e);
                    throw new StarWhaleApiException(new SWProcessException(ErrorType.NETWORK),HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(swdsUploader.create(text,dsFile.getOriginalFilename(),uploadRequest))));
            case BLOB:
                //get ds path and upload to the dest path
                swdsUploader.uploadBody(uploadId,dsFile);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case CANCEL:
                swdsUploader.cancel(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case END:
                swdsUploader.end(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            default:
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("unknown phase " + uploadRequest.getPhase()),HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyDatasetVersionInfo(String projectId, String datasetId,
        String versionId, String tag) {
        Boolean res = swDatasetService.modifySWDSVersion(
            SWDSVersion.builder().id(idConvertor.revert(versionId)).tag(tag).build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(String projectId, String versionId,
        Integer pageNum, Integer pageSize) {
        PageInfo<DatasetVO> pageInfo;
        if(StringUtils.hasText(versionId)) {
            List<DatasetVO> voList = swDatasetService.findDatasetsByVersionIds(
                Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = swDatasetService.listSWDataset(
                SWDSObject.builder().projectId(idConvertor.revert(projectId)).build(),
                PageParams.builder().pageNum(pageNum).pageSize(pageSize).build());
        }
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createDataset(String projectId,
        String datasetName, MultipartFile zipFile, SWDSRequest swdsRequest) {
        User user = userService.currentUserDetail();
        Long datasetId = swDatasetService.addDataset(
            SWDSObject.builder().projectId(idConvertor.revert(projectId)).name(datasetName).ownerId(user.getId())
                .build());
        Long versionId = createVersion(projectId, datasetId, zipFile, swdsRequest.getImportPath(), user.getId());
        return ResponseEntity.ok(Code.success
            .asResponse(String.valueOf(Optional.of(versionId).orElseThrow(ApiOperationException::new))));
    }

    private Long createVersion(String projectId, Long datasetId, MultipartFile zipFile, String importPath, Long userId) {
        String path = importPath;
        String meta = "";
        if (zipFile != null) {
            // upload file
            SWDSFile swdsFile = new SWDSFile(projectId, String.valueOf(datasetId));
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
            .currentVersion(SWDSVersion.builder()
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
