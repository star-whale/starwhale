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
import ai.starwhale.mlops.api.protocol.dataset.DatasetInfoVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetTagRequest;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetViewVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.dataset.RevertDatasetRequest;
import ai.starwhale.mlops.api.protocol.dataset.build.BuildRecordVo;
import ai.starwhale.mlops.api.protocol.dataset.build.DatasetBuildRequest;
import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataConsumptionRequest;
import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.api.protocol.dataset.upload.DatasetUploadRequest;
import ai.starwhale.mlops.api.protocol.upload.UploadResult;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersionQuery;
import ai.starwhale.mlops.domain.dataset.build.bo.CreateBuildRecordRequest;
import ai.starwhale.mlops.domain.dataset.dataloader.DataReadRequest;
import ai.starwhale.mlops.domain.dataset.dataloader.ReadMode;
import ai.starwhale.mlops.domain.dataset.objectstore.HashNamedDatasetObjectStoreFactory;
import ai.starwhale.mlops.domain.dataset.upload.DatasetUploader;
import ai.starwhale.mlops.domain.storage.HashNamedObjectStore;
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
import java.util.Map;
import java.util.Set;
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
@RequestMapping("${sw.controller.api-prefix}")
@Slf4j
public class DatasetController implements DatasetApi {

    private final DatasetService datasetService;
    private final IdConverter idConvertor;
    private final DatasetUploader datasetUploader;
    private final HashNamedDatasetObjectStoreFactory hashNamedDatasetObjectStoreFactory;

    public DatasetController(
            DatasetService datasetService,
            IdConverter idConvertor,
            DatasetUploader datasetUploader,
            HashNamedDatasetObjectStoreFactory hashNamedDatasetObjectStoreFactory
    ) {
        this.datasetService = datasetService;
        this.idConvertor = idConvertor;
        this.datasetUploader = datasetUploader;
        this.hashNamedDatasetObjectStoreFactory = hashNamedDatasetObjectStoreFactory;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> revertDatasetVersion(
            String projectUrl,
            String modelUrl,
            RevertDatasetRequest revertRequest
    ) {
        Boolean res = datasetService.revertVersionTo(projectUrl, modelUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Revert dataset version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDataset(String projectUrl, String datasetUrl) {
        Boolean res = datasetService.deleteDataset(
                DatasetQuery.builder()
                        .projectUrl(projectUrl)
                        .datasetUrl(datasetUrl)
                        .build());
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Delete dataset failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverDataset(String projectUrl, String datasetUrl) {
        Boolean res = datasetService.recoverDataset(projectUrl, datasetUrl);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Recover dataset failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<DatasetInfoVo>> getDatasetInfo(
            String projectUrl,
            String datasetUrl,
            String versionUrl
    ) {
        DatasetInfoVo datasetInfo = datasetService.getDatasetInfo(
                DatasetQuery.builder()
                        .projectUrl(projectUrl)
                        .datasetUrl(datasetUrl)
                        .datasetVersionUrl(versionUrl)
                        .build());

        return ResponseEntity.ok(Code.success.asResponse(datasetInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<DataIndexDesc>> consumeNextData(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            DataConsumptionRequest dataRangeRequest
    ) {
        var dataset = datasetService.query(projectUrl, datasetUrl, versionUrl);

        return ResponseEntity.ok(Code.success.asResponse(datasetService.nextData(
                DataReadRequest.builder()
                        .sessionId(dataRangeRequest.getSessionId())
                        .consumerId(dataRangeRequest.getConsumerId())
                        .isSerial(dataRangeRequest.isSerial())
                        .readMode(ReadMode.values()[dataRangeRequest.getMode()])
                        .datasetName(dataset.getDatasetName())
                        .datasetVersion(dataset.getVersionName())
                        .tableName(dataset.getIndexTable())
                        .start(dataRangeRequest.getStart())
                        .startInclusive(dataRangeRequest.isStartInclusive())
                        .batchSize(dataRangeRequest.getBatchSize())
                        .end(dataRangeRequest.getEnd())
                        .endInclusive(dataRangeRequest.isEndInclusive())
                        .processedData(dataRangeRequest.getProcessedData())
                        .build()
        )));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVo>>> listDatasetVersion(
            String projectUrl,
            String datasetUrl,
            String versionName,
            Integer pageNum,
            Integer pageSize
    ) {
        PageInfo<DatasetVersionVo> pageInfo = datasetService.listDatasetVersionHistory(
                DatasetVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .datasetUrl(datasetUrl)
                        .versionName(versionName)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build()
        );
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<UploadResult>> uploadDs(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            MultipartFile dsFile,
            DatasetUploadRequest uploadRequest
    ) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setSwds(datasetUrl + ":" + versionUrl);
        Long uploadId = uploadRequest.getUploadId();
        String partName = uploadRequest.getPartName();
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
                    throw new StarwhaleApiException(
                            new SwProcessException(ErrorType.NETWORK),
                            HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                return ResponseEntity.ok(Code.success.asResponse(
                        new UploadResult(datasetUploader.create(text, dsFile.getOriginalFilename(), uploadRequest))));
            case BLOB:
                //get ds path and upload to the dest path
                datasetUploader.uploadHashedBlob(uploadId, dsFile, partName);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case CANCEL:
                datasetUploader.cancel(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case END:
                datasetUploader.end(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            default:
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.DATASET, "unknown phase " + uploadRequest.getPhase()),
                        HttpStatus.BAD_REQUEST
                );
        }
    }

    /**
     * legacy blob content download api, use {@link #signLinks(String, String, Set, Long)} or
     * {@link #pullUriContent(String, String, String, Long, Long, HttpServletResponse)} instead
     */
    @Deprecated
    @Override
    public void pullDs(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            String blobHash,
            HttpServletResponse httpResponse
    ) {
        if (!StringUtils.hasText(datasetUrl) || !StringUtils.hasText(versionUrl)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.DATASET, "please provide name and version for the DS "),
                    HttpStatus.BAD_REQUEST
            );
        }
        datasetUploader.pull(projectUrl, datasetUrl, versionUrl, blobHash, httpResponse);
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> uploadHashedBlob(
            String projectUrl,
            String datasetName,
            String hash,
            MultipartFile dsFile
    ) {
        return ResponseEntity.ok(
                Code.success.asResponse(datasetUploader.uploadHashedBlob(projectUrl, datasetName, dsFile, hash)));
    }

    public ResponseEntity<?> headHashedBlob(String project, String datasetName, String hash) {
        HashNamedObjectStore hashNamedObjectStore = hashNamedDatasetObjectStoreFactory.of(project, datasetName);
        String path;
        try {
            path = hashNamedObjectStore.head(hash);
        } catch (IOException e) {
            log.error("access to main object storage failed", e);
            throw new SwProcessException(ErrorType.STORAGE, "access to main object storage failed", e);
        }
        if (null != path) {
            return ResponseEntity.ok().header("X-SW-LOCAL-STORAGE-URI", path).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public void getHashedBlob(String project, String datasetName, String blobHash, HttpServletResponse httpResponse) {
        try (
                var inputStream = hashNamedDatasetObjectStoreFactory.of(project, datasetName).get(blobHash.trim());
                var outputStream = httpResponse.getOutputStream()
        ) {
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + blobHash + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(inputStream.getSize()));
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "pull file from storage failed", e);
        }
    }

    @Override
    public void pullUriContent(
            String project,
            String datasetName,
            String uri,
            Long offset,
            Long size,
            HttpServletResponse httpResponse
    ) {
        try {
            ServletOutputStream outputStream = httpResponse.getOutputStream();
            outputStream.write(datasetService.dataOf(project, datasetName, uri, offset, size));
            outputStream.flush();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.NETWORK, "error write data to response", e);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<Map>> signLinks(
            String project,
            String datasetName,
            Set<String> uris,
            Long expTimeMillis
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                datasetService.signLinks(project, datasetName, uris, expTimeMillis)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> shareDatasetVersion(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            Boolean shared
    ) {
        datasetService.shareDatasetVersion(projectUrl, datasetUrl, versionUrl, shared);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> addDatasetVersionTag(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            DatasetTagRequest datasetTagRequest
    ) {
        datasetService.addDatasetVersionTag(
                projectUrl,
                datasetUrl,
                versionUrl,
                datasetTagRequest.getTag(),
                datasetTagRequest.getForce()
        );
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<String>>> listDatasetVersionTags(
            String projectUrl,
            String datasetUrl,
            String versionUrl
    ) {
        var tags = datasetService.listDatasetVersionTags(projectUrl, datasetUrl, versionUrl);
        return ResponseEntity.ok(Code.success.asResponse(tags));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDatasetVersionTag(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            String tag
    ) {
        datasetService.deleteDatasetVersionTag(projectUrl, datasetUrl, versionUrl, tag);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<Long>> getDatasetVersionTag(
            String projectUrl,
            String datasetUrl,
            String tag
    ) {
        var entity = datasetService.getDatasetVersionTag(projectUrl, datasetUrl, tag);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Code.success.asResponse(entity.getVersionId()));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVo>>> listDataset(
            String projectUrl,
            String versionId,
            String name,
            String owner,
            Integer pageNum,
            Integer pageSize
    ) {
        PageInfo<DatasetVo> pageInfo;
        if (StringUtils.hasText(versionId)) {
            List<DatasetVo> voList = datasetService.findDatasetsByVersionIds(
                    Stream.of(versionId.split("[,;]")).map(idConvertor::revert).collect(
                            Collectors.toList()));
            pageInfo = PageInfo.of(voList);
        } else {
            pageInfo = datasetService.listDataset(
                    DatasetQuery.builder()
                            .projectUrl(projectUrl)
                            .namePrefix(name)
                            .owner(owner)
                            .build(),
                    PageParams.builder()
                            .pageNum(pageNum)
                            .pageSize(pageSize)
                            .build()
            );
        }
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<DatasetViewVo>>> listDatasetTree(String projectUrl) {
        List<DatasetViewVo> datasetViewVos = datasetService.listDatasetVersionView(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(datasetViewVos));
    }

    @Override
    public ResponseEntity<?> headDataset(String projectUrl, String datasetUrl, String versionUrl) {
        try {
            datasetService.query(projectUrl, datasetUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head dataset result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> buildDataset(
            String projectUrl,
            String datasetName,
            DatasetBuildRequest datasetBuildRequest
    ) {
        datasetService.build(CreateBuildRecordRequest.builder()
                                     .datasetId(datasetBuildRequest.getDatasetId())
                                     .datasetName(datasetName)
                                     .shared(datasetBuildRequest.getShared())
                                     .projectUrl(projectUrl)
                                     .type(datasetBuildRequest.getType())
                                     .storagePath(datasetBuildRequest.getStoragePath())
                                     .build());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<BuildRecordVo>>> listBuildRecords(
            String projectUrl,
            Integer pageNum,
            Integer pageSize
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                datasetService.listBuildRecords(projectUrl,  new PageParams(pageNum, pageSize))));
    }


}
