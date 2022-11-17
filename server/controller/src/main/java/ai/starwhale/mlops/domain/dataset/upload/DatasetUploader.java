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

package ai.starwhale.mlops.domain.dataset.upload;

import ai.starwhale.mlops.api.protocol.dataset.upload.UploadRequest;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.dataset.DatasetManager;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.dataset.index.datastore.DataStoreTableNameHelper;
import ai.starwhale.mlops.domain.dataset.index.datastore.IndexWriter;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.upload.bo.DatasetVersionWithMeta;
import ai.starwhale.mlops.domain.dataset.upload.bo.Manifest;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class DatasetUploader {

    /**
     * prefix + / + fileName
     */
    static final String FORMATTER_STORAGE_PATH = "%s/%s";
    static final String INDEX_FILE_NAME = "_meta.jsonl";
    static final Pattern PATTERN_SIGNATURE = Pattern.compile("\\d+:blake2b:(.*)");
    static final String DATASET_MANIFEST = "_manifest.yaml";
    final HotDatasetHolder hotDatasetHolder;
    final DatasetMapper datasetMapper;
    final DatasetVersionMapper datasetVersionMapper;
    final StoragePathCoordinator storagePathCoordinator;
    final StorageAccessService storageAccessService;
    final UserService userService;
    final YAMLMapper yamlMapper;
    final HotJobHolder jobHolder;
    final ProjectManager projectManager;
    final DataStoreTableNameHelper dataStoreTableNameHelper;
    final IndexWriter indexWriter;
    final DatasetManager datasetManager;
    final IdConvertor idConvertor;
    final VersionAliasConvertor versionAliasConvertor;

    public DatasetUploader(HotDatasetHolder hotDatasetHolder, DatasetMapper datasetMapper,
            DatasetVersionMapper datasetVersionMapper, StoragePathCoordinator storagePathCoordinator,
            StorageAccessService storageAccessService, UserService userService,
            YAMLMapper yamlMapper,
            HotJobHolder jobHolder,
            ProjectManager projectManager, DataStoreTableNameHelper dataStoreTableNameHelper,
            IndexWriter indexWriter,
            DatasetManager datasetManager, IdConvertor idConvertor, VersionAliasConvertor versionAliasConvertor) {
        this.hotDatasetHolder = hotDatasetHolder;
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.userService = userService;
        this.yamlMapper = yamlMapper;
        this.jobHolder = jobHolder;
        this.projectManager = projectManager;
        this.dataStoreTableNameHelper = dataStoreTableNameHelper;
        this.indexWriter = indexWriter;
        this.datasetManager = datasetManager;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public void cancel(String uploadId) {
        final DatasetVersionWithMeta swDatasetVersionEntityWithMeta = getDatasetVersion(uploadId);
        datasetVersionMapper.deleteById(swDatasetVersionEntityWithMeta.getDatasetVersionEntity().getId());
        hotDatasetHolder.cancel(uploadId);
        clearDatasetStorageData(swDatasetVersionEntityWithMeta.getDatasetVersionEntity());

    }

    private void clearDatasetStorageData(DatasetVersionEntity datasetVersionEntity) {
        final String storagePath = datasetVersionEntity.getStoragePath();
        try {
            Stream<String> files = storageAccessService.list(storagePath);
            files.parallel().forEach(file -> {
                try {
                    storageAccessService.delete(file);
                } catch (IOException e) {
                    log.error("clear file failed {}", file, e);
                }
            });
        } catch (IOException e) {
            log.error("delete storage objects failed for {}", datasetVersionEntity.getVersionName(), e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void uploadBody(String uploadId, MultipartFile file, String uri) {
        final DatasetVersionWithMeta swDatasetVersionWithMeta = getDatasetVersion(uploadId);
        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            if (INDEX_FILE_NAME.equals(filename)) {
                try (InputStream anotherInputStream = file.getInputStream()) {
                    indexWriter.writeToStore(swDatasetVersionWithMeta.getDatasetVersionEntity().getIndexTable(),
                            anotherInputStream);
                }
            }
            final String storagePath = String.format(FORMATTER_STORAGE_PATH,
                    swDatasetVersionWithMeta.getDatasetVersionEntity().getStoragePath(),
                    StringUtils.hasText(uri) ? uri : filename);
            storageAccessService.put(storagePath, inputStream, file.getSize());
        } catch (IOException e) {
            log.error("read dataset failed {}", filename, e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.NETWORK),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void uploadDatasetFileToStorage(String filename, byte[] fileBytes,
            DatasetVersionEntity datasetVersionEntity) {
        final String storagePath = String.format(FORMATTER_STORAGE_PATH, datasetVersionEntity.getStoragePath(),
                filename);
        try {
            storageAccessService.put(storagePath, fileBytes);
        } catch (IOException e) {
            log.error("upload dataset to failed {}", filename, e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean fileUploaded(DatasetVersionWithMeta datasetVersionWithMeta, String filename,
            String digest) {
        Map<String, String> uploadedFileBlake2bs = datasetVersionWithMeta.getVersionMeta()
                .getUploadedFileBlake2bs();
        return digest.equals(uploadedFileBlake2bs.get(filename));
    }

    DatasetVersionWithMeta getDatasetVersion(String uploadId) {
        final Optional<DatasetVersionWithMeta> swDatasetVersionEntityOpt = hotDatasetHolder.of(uploadId);
        return swDatasetVersionEntityOpt
                .orElseThrow(
                        () -> new StarwhaleApiException(
                                new SwValidationException(ValidSubject.DATASET, "uploadId invalid"),
                                HttpStatus.BAD_REQUEST));
    }

    void uploadManifest(DatasetVersionEntity datasetVersionEntity, String fileName, byte[] bytes) {
        uploadDatasetFileToStorage(fileName, bytes, datasetVersionEntity);
    }

    void reUploadManifest(DatasetVersionEntity datasetVersionEntity, String fileName, byte[] bytes) {
        final String storagePath = String.format(FORMATTER_STORAGE_PATH, datasetVersionEntity.getStoragePath(),
                fileName);
        try {
            storageAccessService.delete(storagePath);
        } catch (IOException e) {
            log.warn("dataset delete to failed {}", fileName, e);
        }
        uploadManifest(datasetVersionEntity, fileName, bytes);
    }

    @Transactional
    public String create(String yamlContent, String fileName, UploadRequest uploadRequest) {
        Manifest manifest;
        try {
            manifest = yamlMapper.readValue(yamlContent, Manifest.class);
            manifest.setRawYaml(yamlContent);
        } catch (JsonProcessingException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.DATASET, "read dataset yaml failed " + fileName, e),
                    HttpStatus.BAD_REQUEST);
        }
        if (null == manifest.getVersion()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.DATASET, "version is required in manifest "),
                    HttpStatus.BAD_REQUEST);
        }
        ProjectEntity projectEntity = projectManager.getProject(uploadRequest.getProject());
        Long projectId = projectEntity.getId();
        DatasetEntity datasetEntity = datasetMapper.findByName(uploadRequest.name(), projectId);
        if (null == datasetEntity) {
            // create
            datasetEntity = from(manifest, uploadRequest.getProject(), uploadRequest.name());
            datasetMapper.addDataset(datasetEntity);
        }
        DatasetVersionEntity datasetVersionEntity = datasetVersionMapper
                .findByDsIdAndVersionNameForUpdate(datasetEntity.getId(), manifest.getVersion());
        if (null == datasetVersionEntity) {
            // create
            datasetVersionEntity = from(projectEntity.getProjectName(), datasetEntity, manifest);
            datasetVersionMapper.addNewVersion(datasetVersionEntity);
            datasetVersionMapper.revertTo(datasetVersionEntity.getDatasetId(), datasetVersionEntity.getId());
            uploadManifest(datasetVersionEntity, fileName, yamlContent.getBytes(StandardCharsets.UTF_8));
        } else {
            // dataset version create dup
            if (datasetVersionEntity.getStatus().equals(DatasetVersionEntity.STATUS_AVAILABLE)) {
                if (uploadRequest.force()) {
                    Set<Long> runningDataSets = jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                            .parallelStream().map(Job::getDataSets)
                            .flatMap(Collection::stream)
                            .map(DataSet::getId)
                            .collect(Collectors.toSet());
                    if (runningDataSets.contains(datasetVersionEntity.getId())) {
                        throw new SwValidationException(ValidSubject.DATASET,
                                " dataset version is being hired by running job, force push is not allowed now");
                    } else {
                        datasetVersionMapper.updateStatus(datasetVersionEntity.getId(),
                                DatasetVersionEntity.STATUS_UN_AVAILABLE);
                    }
                } else {
                    throw new SwValidationException(ValidSubject.DATASET,
                            " same dataset version can't be uploaded twice");
                }

            }
            if (!yamlContent.equals(datasetVersionEntity.getVersionMeta())) {
                datasetVersionEntity.setVersionMeta(yamlContent);
                // if manifest(signature) change, all files should be re-uploaded
                datasetVersionEntity.setFilesUploaded("");
                clearDatasetStorageData(datasetVersionEntity);
                datasetVersionMapper.updateFilesUploaded(datasetVersionEntity);
                reUploadManifest(datasetVersionEntity, fileName, yamlContent.getBytes(StandardCharsets.UTF_8));
            }
        }
        hotDatasetHolder.manifest(datasetVersionEntity);
        return datasetVersionEntity.getVersionName();
    }

    private DatasetVersionEntity from(String projectName, DatasetEntity datasetEntity, Manifest manifest) {
        return DatasetVersionEntity.builder().datasetId(datasetEntity.getId())
                .ownerId(getOwner())
                .storagePath(storagePathCoordinator.allocateDatasetPath(projectName, datasetEntity.getDatasetName(),
                        manifest.getVersion()))
                .versionMeta(manifest.getRawYaml())
                .versionName(manifest.getVersion())
                .size(manifest.getDatasetSummary().getRows())
                .indexTable(dataStoreTableNameHelper.tableNameOfDataset(projectName, datasetEntity.getDatasetName(),
                        manifest.getVersion()))
                .filesUploaded(DatasetVersionWithMetaConverter.EMPTY_YAML)
                .build();
    }

    private DatasetEntity from(Manifest manifest, String project, String datasetName) {
        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project,
                userService.currentUserDetail().getIdTableKey());
        return DatasetEntity.builder()
                .datasetName(datasetName)
                .isDeleted(0)
                .ownerId(getOwner())
                .projectId(projectEntity == null ? null : projectEntity.getId())
                .build();
    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if (null == currentUserDetail) {
            throw new StarwhaleApiException(new SwAuthException(SwAuthException.AuthType.DATASET_UPLOAD),
                    HttpStatus.FORBIDDEN);
        }
        return currentUserDetail.getIdTableKey();
    }

    public void end(String uploadId) {
        final DatasetVersionWithMeta datasetVersionWithMeta = getDatasetVersion(uploadId);
        datasetVersionMapper.updateStatus(datasetVersionWithMeta.getDatasetVersionEntity().getId(),
                DatasetVersionEntity.STATUS_AVAILABLE);
        hotDatasetHolder.end(uploadId);
    }

    public void pull(String project, String name, String version, String partName, HttpServletResponse httpResponse) {
        BundleManager bundleManager = new BundleManager(idConvertor, versionAliasConvertor,
                projectManager, datasetManager, datasetManager);
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(project, name, version));
        DatasetVersionEntity datasetVersionEntity = datasetVersionMapper.getVersionById(versionId);
        if (null == datasetVersionEntity) {
            throw new SwValidationException(ValidSubject.DATASET, "dataset version doesn't exists");
        }
        if (!StringUtils.hasText(partName)) {
            partName = DATASET_MANIFEST;
        }
        try (InputStream inputStream = storageAccessService.get(
                datasetVersionEntity.getStoragePath() + "/" + partName.trim());
                ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = inputStream.transferTo(outputStream);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + fileNameFromUri(partName) + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "pull file from storage failed", e);
        }
    }

    String fileNameFromUri(String uri) {
        String[] split = uri.split("/");
        return split[split.length - 1];
    }
}
