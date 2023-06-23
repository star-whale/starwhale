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

import ai.starwhale.mlops.api.protocol.dataset.upload.DatasetUploadRequest;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.index.datastore.DataStoreTableNameHelper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.upload.bo.DatasetVersionWithMeta;
import ai.starwhale.mlops.domain.dataset.upload.bo.Manifest;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.storage.HashNamedObjectStore;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    static final String DATASET_MANIFEST = "_manifest.yaml";
    final HotDatasetHolder hotDatasetHolder;
    final DatasetMapper datasetMapper;
    final DatasetVersionMapper datasetVersionMapper;
    final StoragePathCoordinator storagePathCoordinator;
    final StorageAccessService storageAccessService;
    final UserService userService;
    final HotJobHolder jobHolder;
    final ProjectService projectService;
    final DataStoreTableNameHelper dataStoreTableNameHelper;
    final DatasetDao datasetDao;
    final IdConverter idConvertor;
    final VersionAliasConverter versionAliasConvertor;

    public DatasetUploader(HotDatasetHolder hotDatasetHolder, DatasetMapper datasetMapper,
            DatasetVersionMapper datasetVersionMapper, StoragePathCoordinator storagePathCoordinator,
            StorageAccessService storageAccessService, UserService userService,
            HotJobHolder jobHolder,
            ProjectService projectService, DataStoreTableNameHelper dataStoreTableNameHelper,
            DatasetDao datasetDao, IdConverter idConvertor, VersionAliasConverter versionAliasConvertor) {
        this.hotDatasetHolder = hotDatasetHolder;
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.userService = userService;
        this.jobHolder = jobHolder;
        this.projectService = projectService;
        this.dataStoreTableNameHelper = dataStoreTableNameHelper;
        this.datasetDao = datasetDao;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public void cancel(Long uploadId) {
        final DatasetVersionWithMeta swDatasetVersionEntityWithMeta = getDatasetVersion(uploadId);
        datasetVersionMapper.delete(swDatasetVersionEntityWithMeta.getDatasetVersion().getId());
        hotDatasetHolder.cancel(uploadId);
    }

    public void uploadHashedBlob(Long uploadId, MultipartFile file, String blobHash) {
        final DatasetVersionWithMeta swDatasetVersionWithMeta = getDatasetVersion(uploadId);
        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            HashNamedObjectStore hashNamedObjectStore = new HashNamedObjectStore(storageAccessService,
                    swDatasetVersionWithMeta.getDatasetVersion().getStoragePath());
            hashNamedObjectStore.put(StringUtils.hasText(blobHash) ? blobHash : filename, inputStream);
        } catch (IOException e) {
            log.error("read dataset failed {}", filename, e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.NETWORK),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public String uploadHashedBlob(String projectName, String datasetName, MultipartFile file, String blobHash) {
        Long projectId = projectService.findProject(projectName).getId();
        String datasetBlobPath = storagePathCoordinator.allocateDatasetPath(projectId, datasetName);
        HashNamedObjectStore hashNamedObjectStore = new HashNamedObjectStore(storageAccessService, datasetBlobPath);
        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            return hashNamedObjectStore.put(StringUtils.hasText(blobHash) ? blobHash : filename, inputStream);
        } catch (IOException e) {
            log.error("write dataset blob file failed {}", filename, e);
            throw new SwProcessException(ErrorType.NETWORK, "write dataset blob file failed", e);
        }
    }

    DatasetVersionWithMeta getDatasetVersion(Long uploadId) {
        final Optional<DatasetVersionWithMeta> swDatasetVersionEntityOpt = hotDatasetHolder.of(uploadId);
        return swDatasetVersionEntityOpt
                .orElseThrow(
                        () -> new StarwhaleApiException(
                                new SwValidationException(ValidSubject.DATASET, "uploadId invalid"),
                                HttpStatus.BAD_REQUEST));
    }

    @Transactional
    public Long create(String yamlContent, String fileName, DatasetUploadRequest uploadRequest) {
        Manifest manifest;
        try {
            manifest = Constants.yamlMapper.readValue(yamlContent, Manifest.class);
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
        Project project = projectService.findProject(uploadRequest.getProject());
        Long projectId = project.getId();
        DatasetEntity datasetEntity = datasetMapper.findByName(uploadRequest.name(), projectId, true);
        if (null == datasetEntity) {
            // create
            datasetEntity = from(manifest, uploadRequest.getProject(), uploadRequest.name());
            datasetMapper.insert(datasetEntity);
        }
        DatasetVersionEntity datasetVersionEntity = datasetVersionMapper
                .findByNameAndDatasetId(manifest.getVersion(), datasetEntity.getId(), true);
        if (null == datasetVersionEntity) {
            // create
            datasetVersionEntity = from(project.getId(), datasetEntity, manifest);
            datasetVersionMapper.insert(datasetVersionEntity);
            RevertManager.create(new BundleManager(
                    idConvertor,
                    versionAliasConvertor,
                    projectService,
                    datasetDao,
                    datasetDao
            ), datasetDao).revertVersionTo(datasetEntity.getId(), datasetVersionEntity.getId());
        } else {
            // dataset already created
            if (datasetVersionEntity.getStatus().equals(DatasetVersion.STATUS_AVAILABLE)) {
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
                        datasetVersionEntity.setVersionMeta(yamlContent);
                        datasetVersionMapper.update(datasetVersionEntity);
                        datasetVersionMapper.updateStatus(datasetVersionEntity.getId(),
                                DatasetVersion.STATUS_UN_AVAILABLE);
                    }
                } else {
                    throw new SwValidationException(ValidSubject.DATASET,
                            " same dataset version can't be uploaded twice without force option");
                }
            } else {
                // dataset is being created
                datasetVersionEntity.setVersionMeta(yamlContent);
                datasetVersionMapper.update(datasetVersionEntity);
            }
        }

        hotDatasetHolder.manifest(DatasetVersion.fromEntity(datasetEntity, datasetVersionEntity));

        return datasetVersionEntity.getId();
    }

    private DatasetVersionEntity from(Long projectId, DatasetEntity datasetEntity, Manifest manifest) {
        return DatasetVersionEntity.builder().datasetId(datasetEntity.getId())
                .ownerId(getOwner())
                .storagePath(storagePathCoordinator.allocateDatasetPath(projectId, datasetEntity.getDatasetName()))
                .versionMeta(manifest.getRawYaml())
                .versionName(manifest.getVersion())
                .size(manifest.getDatasetSummary().getRows())
                .indexTable(dataStoreTableNameHelper.tableNameOfDataset(projectId, datasetEntity.getDatasetName()))
                .filesUploaded(DatasetVersionWithMetaConverter.EMPTY_YAML)
                .build();
    }

    private DatasetEntity from(Manifest manifest, String project, String datasetName) {
        Project pro = projectService.findProject(project);
        return DatasetEntity.builder()
                .datasetName(datasetName)
                .isDeleted(0)
                .ownerId(getOwner())
                .projectId(pro == null ? null : pro.getId())
                .build();
    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if (null == currentUserDetail) {
            throw new StarwhaleApiException(new SwAuthException(SwAuthException.AuthType.DATASET_UPLOAD),
                    HttpStatus.FORBIDDEN);
        }
        return currentUserDetail.getId();
    }

    public void end(Long uploadId) {
        final DatasetVersionWithMeta datasetVersionWithMeta = getDatasetVersion(uploadId);
        datasetVersionMapper.updateStatus(datasetVersionWithMeta.getDatasetVersion().getId(),
                DatasetVersion.STATUS_AVAILABLE);
        hotDatasetHolder.end(uploadId);
    }


    /**
     * legacy dataset file pull interface. Use
     */
    public void pull(String project, String name, String version, String blobHash, HttpServletResponse httpResponse) {
        BundleManager bundleManager = new BundleManager(idConvertor, versionAliasConvertor,
                projectService, datasetDao, datasetDao);
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(project, name, version));
        DatasetVersionEntity datasetVersionEntity = datasetVersionMapper.find(versionId);
        if (null == datasetVersionEntity) {
            throw new SwValidationException(ValidSubject.DATASET, "dataset version doesn't exists");
        }
        if (!StringUtils.hasText(blobHash) || DATASET_MANIFEST.equalsIgnoreCase(blobHash)) {
            try (ServletOutputStream outputStream = httpResponse.getOutputStream()) {
                String versionMeta = datasetVersionEntity.getVersionMeta();
                outputStream.write(versionMeta.getBytes(StandardCharsets.UTF_8));
                httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + DATASET_MANIFEST + "\"");
                httpResponse.addHeader("Content-Length", String.valueOf(versionMeta.length()));
                outputStream.flush();
            } catch (IOException e) {
                throw new SwProcessException(ErrorType.STORAGE, "pull file from storage failed", e);
            }

        } else {
            try (InputStream inputStream = new HashNamedObjectStore(storageAccessService,
                    datasetVersionEntity.getStoragePath()).get(blobHash.trim());
                    ServletOutputStream outputStream = httpResponse.getOutputStream()) {
                long length = inputStream.transferTo(outputStream);
                httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + blobHash + "\"");
                httpResponse.addHeader("Content-Length", String.valueOf(length));
                outputStream.flush();
            } catch (IOException e) {
                throw new SwProcessException(ErrorType.STORAGE, "pull file from storage failed", e);
            }
        }
    }
}
