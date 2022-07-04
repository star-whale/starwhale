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
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.api.protocol.swds.RevertSWDSRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDSTagRequest;
import ai.starwhale.mlops.api.protocol.swds.SWDatasetInfoVO;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadResult;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.swds.bo.SWDSQuery;
import ai.starwhale.mlops.domain.swds.bo.SWDSVersion;
import ai.starwhale.mlops.domain.swds.bo.SWDSVersionQuery;
import ai.starwhale.mlops.domain.swds.SWDatasetService;
import ai.starwhale.mlops.domain.swds.upload.SwdsUploader;
import ai.starwhale.mlops.exception.ApiOperationException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.lang.Assert;
import com.github.pagehelper.PageInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
@Slf4j
public class DatasetController implements DatasetApi{

    @Resource
    private SWDatasetService swDatasetService;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private SwdsUploader swdsUploader;

    @Override
    public ResponseEntity<ResponseMessage<String>> revertDatasetVersion(String projectUrl,
        String swmpUrl, RevertSWDSRequest revertRequest) {
        Boolean res = swDatasetService.revertVersionTo(projectUrl, swmpUrl, revertRequest.getVersion());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Revert swds version failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDataset(String projectUrl,
        String datasetUrl) {
        Boolean res = swDatasetService.deleteSWDS(
            SWDSQuery.builder()
                .projectUrl(projectUrl)
                .swdsUrl(datasetUrl)
                .build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete swds failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverDataset(String projectUrl,
        String datasetUrl) {
        Boolean res = swDatasetService.recoverSWDS(projectUrl, datasetUrl);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Recover dataset failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SWDatasetInfoVO>> getDatasetInfo(String projectUrl,
        String datasetUrl, String versionUrl) {
        SWDatasetInfoVO swdsInfo = swDatasetService.getSWDSInfo(
            SWDSQuery.builder()
                .projectUrl(projectUrl)
                .swdsUrl(datasetUrl)
                .swdsVersionUrl(versionUrl)
                .build());

        return ResponseEntity.ok(Code.success.asResponse(swdsInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(
        String projectUrl, String datasetUrl, String vName, String tag, Integer pageNum, Integer pageSize) {
        PageInfo<DatasetVersionVO> pageInfo = swDatasetService.listDatasetVersionHistory(
            SWDSVersionQuery.builder()
                .projectUrl(projectUrl)
                .swdsUrl(datasetUrl)
                .versionName(vName)
                .versionTag(tag)
                .build(),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
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
    public byte[] pullDS(String project, String name, String version,
        String partName, HttpServletResponse httpResponse) {
        if(!StringUtils.hasText(name) || !StringUtils.hasText(version) ){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("please provide name and version for the DS "),HttpStatus.BAD_REQUEST);
        }
        return swdsUploader.pull(project, name,version,partName, httpResponse);
    }

    @Override
    public ResponseEntity<ResponseMessage<List<SWDatasetInfoVO>>> listDS(String project,
        String name) {
        return ResponseEntity.ok(Code.success.asResponse(swDatasetService.listDS(project,name)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyDatasetVersionInfo(String projectUrl, String datasetUrl,
        String versionUrl, SWDSTagRequest swdsTagRequest) {
        Boolean res = swDatasetService.modifySWDSVersion(projectUrl, datasetUrl, versionUrl,
            SWDSVersion.builder().tag(swdsTagRequest.getTag()).build());
        Assert.isTrue(Optional.of(res).orElseThrow(ApiOperationException::new));
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> manageDatasetTag(String projectUrl,
        String datasetUrl, String versionUrl, SWDSTagRequest swdsTagRequest) {
        TagAction ta;
        try {
            ta = TagAction.of(swdsTagRequest.getAction(), swdsTagRequest.getTag());
        } catch (IllegalArgumentException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip(String.format("Unknown tag action %s ", swdsTagRequest.getAction())),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = swDatasetService.manageVersionTag(projectUrl, datasetUrl, versionUrl, ta);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update dataset tag failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(String projectUrl, String versionId,
        Integer pageNum, Integer pageSize) {
        PageInfo<DatasetVO> pageInfo;
        if(StringUtils.hasText(versionId)) {
            List<DatasetVO> voList = swDatasetService.findDatasetsByVersionIds(
                Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = swDatasetService.listSWDataset(
                SWDSQuery.builder()
                    .projectUrl(projectUrl)
                    .build(),
                PageParams.builder()
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .build());
        }
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<String> headDataset(UploadRequest uploadRequest) {
        return ResponseEntity.ok(swDatasetService.query(uploadRequest));
    }
}
