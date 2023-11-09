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

import static ai.starwhale.mlops.domain.bundle.BundleManager.BUNDLE_NAME_REGEX;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.NullableResponseMessage;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.bundle.DataScope;
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
import ai.starwhale.mlops.domain.dataset.objectstore.HashNamedDatasetObjectStoreFactory;
import ai.starwhale.mlops.domain.dataset.upload.DatasetUploader;
import ai.starwhale.mlops.domain.storage.HashNamedObjectStore;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("${sw.controller.api-prefix}")
@Tag(name = "Dataset")
@Validated
public class DatasetController {

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

    @Operation(summary = "Revert Dataset version",
            description = "Select a historical version of the dataset and revert the latest version of the current "
                    + "dataset to this version")
    @PostMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/revert",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> revertDatasetVersion(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @Valid @RequestBody RevertDatasetRequest revertRequest
    ) {
        Boolean res = datasetService.revertVersionTo(projectUrl, datasetUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Revert dataset version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Delete a dataset")
    @DeleteMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteDataset(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl
    ) {
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

    @Operation(summary = "Recover a dataset")
    @PutMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/recover",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverDataset(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl
    ) {
        Boolean res = datasetService.recoverDataset(projectUrl, datasetUrl);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Recover dataset failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Get the information of a dataset",
            description = "Return the information of the latest version of the current dataset")
    @GetMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<DatasetInfoVo>> getDatasetInfo(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Dataset versionUrl. "
                    + "(Return the current version as default when the versionUrl is not set.)"
            )
            @Valid
            @RequestParam(value = "versionUrl", required = false)
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

    @PostMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/consume",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<NullableResponseMessage<DataIndexDesc>> consumeNextData(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl,
            @RequestBody DataConsumptionRequest dataRangeRequest
    ) {
        var dataset = datasetService.query(projectUrl, datasetUrl, versionUrl);

        return ResponseEntity.ok(Code.success.asNullableResponse(datasetService.nextData(
                DataReadRequest.builder()
                        .sessionId(dataRangeRequest.getSessionId())
                        .consumerId(dataRangeRequest.getConsumerId())
                        .datasetVersion(dataset)
                        .start(dataRangeRequest.getStart())
                        .startType(dataRangeRequest.getStartType())
                        .startInclusive(dataRangeRequest.isStartInclusive())
                        .batchSize(dataRangeRequest.getBatchSize())
                        .end(dataRangeRequest.getEnd())
                        .endType(dataRangeRequest.getEndType())
                        .endInclusive(dataRangeRequest.isEndInclusive())
                        .processedData(dataRangeRequest.getProcessedData())
                        .build()
        )));
    }

    @Operation(summary = "Get the list of the dataset versions")
    @GetMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVo>>> listDatasetVersion(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<DatasetVersionVo> pageInfo = datasetService.listDatasetVersionHistory(
                DatasetVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .datasetUrl(datasetUrl)
                        .versionName(name)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build()
        );
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    /**
     * use #uploadHashedBlob instead
     */
    @Operation(summary = "Create a new dataset version",
            description = "Create a new version of the dataset. "
                    + "The data resources can be selected by uploading the file package or entering the server path.")
    @PostMapping(
            value = "/project/{projectUrl}/dataset/{datasetName}/version/{versionName}/file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    @Deprecated
    ResponseEntity<ResponseMessage<UploadResult>> uploadDs(
            @PathVariable String projectUrl,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Dataset name is invalid")
            @PathVariable String datasetName,
            @PathVariable String versionName,
            @RequestPart(value = "file", required = false) MultipartFile dsFile,
            DatasetUploadRequest uploadRequest
    ) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setSwds(datasetName + ":" + versionName);
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
            case CANCEL:
                datasetUploader.cancel(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case END:
                datasetUploader.end(uploadId);
                return ResponseEntity.ok(Code.success.asResponse(new UploadResult(uploadId)));
            case BLOB:
                //this phase is abandoned
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
    @Operation(summary = "Pull Dataset files", description = "Pull Dataset files part by part. ")
    @GetMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/file",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    void pullDs(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl,
            @Parameter(description = "optional, _manifest.yaml is used if not specified")
            @RequestParam(required = false) String partName,
            HttpServletResponse httpResponse
    ) {
        if (!StringUtils.hasText(datasetUrl) || !StringUtils.hasText(versionUrl)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.DATASET, "please provide name and version for the DS "),
                    HttpStatus.BAD_REQUEST
            );
        }
        datasetUploader.pull(projectUrl, datasetUrl, versionUrl, partName, httpResponse);
    }

    @Operation(summary = "Upload a hashed BLOB to dataset object store",
            description = "Upload a hashed BLOB to dataset object store, returns a uri of the main storage")
    @PostMapping(
            value = "/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> uploadHashedBlob(
            @PathVariable String projectName,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Dataset name is invalid")
            @PathVariable String datasetName,
            @PathVariable String hash,
            @RequestPart(value = "file") MultipartFile dsFile
    ) {
        return ResponseEntity.ok(
                Code.success.asResponse(datasetUploader.uploadHashedBlob(projectName, datasetName, dsFile, hash)));
    }

    @Operation(summary = "Test if a hashed blob exists in this dataset",
            description = "404 if not exists; 200 if exists")
    @RequestMapping(
            value = "/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}",
            method = RequestMethod.HEAD,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headHashedBlob(
            @PathVariable String projectName,
            @PathVariable String datasetName,
            @PathVariable String hash
    ) {
        HashNamedObjectStore hashNamedObjectStore = hashNamedDatasetObjectStoreFactory.of(projectName, datasetName);
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

    @Operation(summary = "Download the hashed blob in this dataset",
            description = "404 if not exists; 200 if exists")
    @RequestMapping(
            value = "/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    void getHashedBlob(
            @PathVariable String projectName,
            @PathVariable String datasetName,
            @PathVariable String hash,
            HttpServletResponse httpResponse
    ) {
        try (
                var inputStream = hashNamedDatasetObjectStoreFactory.of(projectName, datasetName).get(hash.trim());
                var outputStream = httpResponse.getOutputStream()
        ) {
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + hash + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(inputStream.getSize()));
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "pull file from storage failed", e);
        }
    }

    @Operation(summary = "Pull Dataset uri file contents", description = "Pull Dataset uri file contents ")
    @GetMapping(
            value = "/project/{projectName}/dataset/{datasetName}/uri",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    void pullUriContent(
            @PathVariable String projectName,
            @PathVariable String datasetName,
            @Parameter(required = true) String uri,
            @RequestParam(required = false) Long offset,
            @RequestParam(required = false) Long size,
            HttpServletResponse httpResponse
    ) {
        try {
            ServletOutputStream outputStream = httpResponse.getOutputStream();
            outputStream.write(datasetService.dataOf(uri, offset, size));
            outputStream.flush();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.NETWORK, "error write data to response", e);
        }
    }

    /**
     * legacy sign links api, use {@link FileStorageController} instead
     */
    @Operation(summary = "Sign SWDS uris to get a batch of temporarily accessible links",
            description = "Sign SWDS uris to get a batch of temporarily accessible links")
    @PostMapping(
            value = "/project/{projectName}/dataset/{datasetName}/uri/sign-links",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    @Deprecated(since = "0.6.2")
    ResponseEntity<ResponseMessage<Map>> signLinks(
            @PathVariable String projectName,
            @PathVariable String datasetName,
            @RequestBody Set<String> uris,
            @Parameter(description = "the link will be expired after expTimeMillis")
            @RequestParam(required = false)
            Long expTimeMillis
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                datasetService.signLinks(projectName, datasetName, uris, expTimeMillis)));
    }

    @Operation(summary = "Share or unshare the dataset version")
    @PutMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/shared",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> shareDatasetVersion(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl,
            @Parameter(
                    in = ParameterIn.QUERY,
                    required = true,
                    description = "1 or true - shared, 0 or false - unshared"
            )
            @RequestParam Boolean shared
    ) {
        datasetService.shareDatasetVersion(projectUrl, datasetUrl, versionUrl, shared);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @PostMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> addDatasetVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody DatasetTagRequest datasetTagRequest
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

    @GetMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<String>>> listDatasetVersionTags(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl
    ) {
        var tags = datasetService.listDatasetVersionTags(projectUrl, datasetUrl, versionUrl);
        return ResponseEntity.ok(Code.success.asResponse(tags));
    }

    @DeleteMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteDatasetVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl,
            @PathVariable String tag
    ) {
        datasetService.deleteDatasetVersionTag(projectUrl, datasetUrl, versionUrl, tag);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/dataset/{datasetUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Long>> getDatasetVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String tag
    ) {
        var entity = datasetService.getDatasetVersionTag(projectUrl, datasetUrl, tag);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Code.success.asResponse(entity.getVersionId()));
    }

    @Operation(summary = "Get the list of the datasets")
    @GetMapping(
            value = "/project/{projectUrl}/dataset",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<DatasetVo>>> listDataset(
            @PathVariable String projectUrl,
            @RequestParam(required = false) String versionId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
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
                            .name(name)
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

    @Operation(summary = "List dataset tree including global datasets")
    @GetMapping(value = "/project/{projectUrl}/dataset-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<DatasetViewVo>>> listDatasetTree(
            @PathVariable String projectUrl,
            @RequestParam(required = false, defaultValue = "all") DataScope scope
    ) {
        List<DatasetViewVo> list;
        switch (scope) {
            case all:
                list = datasetService.listDatasetVersionView(projectUrl, true, true);
                break;
            case shared:
                list = datasetService.listDatasetVersionView(projectUrl, true, false);
                break;
            case project:
                list = datasetService.listDatasetVersionView(projectUrl, false, true);
                break;
            default:
                list = List.of();
        }
        return ResponseEntity.ok(Code.success.asResponse(list));
    }

    @GetMapping(value = "/project/{projectUrl}/recent-dataset-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<DatasetViewVo>>> recentDatasetTree(
            @PathVariable String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Data limit")
            @RequestParam(required = false, defaultValue = "5")
            @Valid
            @Min(value = 1, message = "limit must be greater than or equal to 1")
            @Max(value = 50, message = "limit must be less than or equal to 50")
            Integer limit
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                datasetService.listRecentlyDatasetVersionView(projectUrl, limit)
        ));
    }

    @Operation(summary = "head for dataset info ",
            description = "head for dataset info")
    @RequestMapping(
            value = "/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headDataset(
            @PathVariable String projectUrl,
            @PathVariable String datasetUrl,
            @PathVariable String versionUrl
    ) {
        try {
            datasetService.query(projectUrl, datasetUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head dataset result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Build Dataset", description = "Build Dataset")
    @PostMapping("/project/{projectUrl}/dataset/{datasetName}/build")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> buildDataset(
            @PathVariable String projectUrl,
            @PathVariable String datasetName,
            @Valid @RequestBody DatasetBuildRequest datasetBuildRequest
    ) {
        var buildRequest = CreateBuildRecordRequest.builder()
                .datasetName(datasetName)
                .shared(datasetBuildRequest.getShared())
                .projectUrl(projectUrl)
                .type(datasetBuildRequest.getType())
                .storagePath(datasetBuildRequest.getStoragePath())
                .build();
        // TODO: add more details for csv,json,hf
        datasetService.build(buildRequest);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "List Build Records", description = "List Build Records")
    @GetMapping("/project/{projectUrl}/dataset/build/list")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<PageInfo<BuildRecordVo>>> listBuildRecords(
            @PathVariable String projectUrl,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                datasetService.listBuildRecords(projectUrl, new PageParams(pageNum, pageSize))));
    }
}
