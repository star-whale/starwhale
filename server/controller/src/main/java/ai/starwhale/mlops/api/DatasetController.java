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
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.swds.DatasetVo;
import ai.starwhale.mlops.api.protocol.swds.RevertSwdsRequest;
import ai.starwhale.mlops.api.protocol.swds.SwDatasetInfoVo;
import ai.starwhale.mlops.api.protocol.swds.SwdsTagRequest;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadResult;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.swds.SwDatasetService;
import ai.starwhale.mlops.domain.swds.bo.SwdsQuery;
import ai.starwhale.mlops.domain.swds.bo.SwdsVersion;
import ai.starwhale.mlops.domain.swds.bo.SwdsVersionQuery;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.upload.SwdsUploader;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
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
public class DatasetController implements DatasetApi {

    private final SwDatasetService swDatasetService;
    private final IdConvertor idConvertor;
    private final SwdsUploader swdsUploader;

    public DatasetController(SwDatasetService swDatasetService, IdConvertor idConvertor, SwdsUploader swdsUploader) {
        this.swDatasetService = swDatasetService;
        this.idConvertor = idConvertor;
        this.swdsUploader = swdsUploader;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> revertDatasetVersion(String projectUrl,
            String swmpUrl, RevertSwdsRequest revertRequest) {
        Boolean res = swDatasetService.revertVersionTo(projectUrl, swmpUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Revert swds version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDataset(String projectUrl,
            String datasetUrl) {
        Boolean res = swDatasetService.deleteSwds(
                SwdsQuery.builder()
                        .projectUrl(projectUrl)
                        .swdsUrl(datasetUrl)
                        .build());
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Delete swds failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverDataset(String projectUrl,
            String datasetUrl) {
        Boolean res = swDatasetService.recoverSwds(projectUrl, datasetUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Recover dataset failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SwDatasetInfoVo>> getDatasetInfo(String projectUrl,
            String datasetUrl, String versionUrl) {
        SwDatasetInfoVo swdsInfo = swDatasetService.getSwdsInfo(
                SwdsQuery.builder()
                        .projectUrl(projectUrl)
                        .swdsUrl(datasetUrl)
                        .swdsVersionUrl(versionUrl)
                        .build());

        return ResponseEntity.ok(Code.success.asResponse(swdsInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVo>>> listDatasetVersion(
            String projectUrl, String datasetUrl, String versionName, String tag, Integer pageNum, Integer pageSize) {
        PageInfo<DatasetVersionVo> pageInfo = swDatasetService.listDatasetVersionHistory(
                SwdsVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .swdsUrl(datasetUrl)
                        .versionName(versionName)
                        .versionTag(tag)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<UploadResult>> uploadDs(String uploadId, String uri,
            String projectUrl, String datasetUrl, String versionUrl,
            MultipartFile dsFile, UploadRequest uploadRequest) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setSwds(datasetUrl + ":" + versionUrl);
        switch (uploadRequest.getPhase()) {
            case MANIFEST:
                String text;
                try (final InputStream inputStream = dsFile.getInputStream()) {
                    text = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    log.error("read manifest file failed", e);
                    throw new StarwhaleApiException(new SwProcessException(ErrorType.NETWORK),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return ResponseEntity.ok(Code.success.asResponse(
                        new UploadResult(swdsUploader.create(text, dsFile.getOriginalFilename(), uploadRequest))));
            case BLOB:
                //get ds path and upload to the dest path
                swdsUploader.uploadBody(uploadId, dsFile, uri);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case CANCEL:
                swdsUploader.cancel(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case END:
                swdsUploader.end(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            default:
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.SWDS).tip("unknown phase " + uploadRequest.getPhase()),
                        HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void pullDs(String projectUrl, String datasetUrl, String versionUrl,
            String partName, HttpServletResponse httpResponse) {
        if (!StringUtils.hasText(datasetUrl) || !StringUtils.hasText(versionUrl)) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS)
                    .tip("please provide name and version for the DS "), HttpStatus.BAD_REQUEST);
        }
        swdsUploader.pull(projectUrl, datasetUrl, versionUrl, partName, httpResponse);
    }

    @Override
    public void pullLinkContent(String projectUrl, String datasetUrl, String versionUrl,
            String uri, String authName, String offset, String size, HttpServletResponse httpResponse) {
        if (!StringUtils.hasText(datasetUrl) || !StringUtils.hasText(versionUrl)) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS)
                    .tip("please provide name and version for the DS "), HttpStatus.BAD_REQUEST);
        }
        SwDatasetVersionEntity datasetVersionEntity = swDatasetService.query(projectUrl, datasetUrl, versionUrl);
        try {
            ServletOutputStream outputStream = httpResponse.getOutputStream();
            outputStream.write(swDatasetService.dataOf(datasetVersionEntity.getId(), uri, authName, offset, size));
            outputStream.flush();
        } catch (IOException e) {
            log.error("error write data to response", e);
            throw new SwProcessException(ErrorType.NETWORK).tip("error write data to response");
        }

    }

    @Override
    public ResponseEntity<ResponseMessage<String>> signLink(String projectUrl, String datasetUrl, String versionUrl,
            String uri, String authName, Long expTimeMillis) {
        SwDatasetVersionEntity datasetVersionEntity = swDatasetService.query(projectUrl, datasetUrl, versionUrl);
        return ResponseEntity.ok(Code.success.asResponse(
                swDatasetService.signLink(datasetVersionEntity.getId(), uri, authName, expTimeMillis)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyDatasetVersionInfo(
            String projectUrl, String datasetUrl, String versionUrl, SwdsTagRequest swdsTagRequest) {
        Boolean res = swDatasetService.modifySwdsVersion(projectUrl, datasetUrl, versionUrl,
                SwdsVersion.builder().tag(swdsTagRequest.getTag()).build());
        if (!res) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS).tip("Modify dataset failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> manageDatasetTag(String projectUrl,
            String datasetUrl, String versionUrl, SwdsTagRequest swdsTagRequest) {
        TagAction ta;
        try {
            ta = TagAction.of(swdsTagRequest.getAction(), swdsTagRequest.getTag());
        } catch (IllegalArgumentException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS).tip(
                    String.format("Unknown tag action %s ", swdsTagRequest.getAction())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Boolean res = swDatasetService.manageVersionTag(projectUrl, datasetUrl, versionUrl, ta);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Update dataset tag failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVo>>> listDataset(String projectUrl, String versionId,
            Integer pageNum, Integer pageSize) {
        PageInfo<DatasetVo> pageInfo;
        if (StringUtils.hasText(versionId)) {
            List<DatasetVo> voList = swDatasetService.findDatasetsByVersionIds(
                    Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                            Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = swDatasetService.listSwDataset(
                    SwdsQuery.builder()
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
    public ResponseEntity<?> headDataset(String projectUrl, String datasetUrl, String versionUrl) {
        try {
            swDatasetService.query(projectUrl, datasetUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head dataset result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }
}
