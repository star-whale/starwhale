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

package ai.starwhale.mlops.domain.model;

import ai.starwhale.mlops.api.protocol.StorageFileVo;
import ai.starwhale.mlops.api.protocol.model.ClientModelRequest;
import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import ai.starwhale.mlops.domain.bundle.tag.TagManager;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ModelService {

    private final ModelMapper modelMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final IdConvertor idConvertor;
    private final VersionAliasConvertor versionAliasConvertor;
    private final ModelConvertor modelConvertor;
    private final ModelVersionConvertor versionConvertor;
    private final StoragePathCoordinator storagePathCoordinator;
    private final StorageAccessService storageAccessService;
    private final StorageService storageService;
    private final UserService userService;
    private final ProjectManager projectManager;
    private final ModelManager modelManager;
    private final HotJobHolder jobHolder;

    private final TrashService trashService;
    @Setter
    private BundleManager bundleManager;

    public ModelService(ModelMapper modelMapper, ModelVersionMapper modelVersionMapper,
            IdConvertor idConvertor, VersionAliasConvertor versionAliasConvertor, ModelConvertor modelConvertor,
            ModelVersionConvertor versionConvertor, StoragePathCoordinator storagePathCoordinator,
            ModelManager modelManager, StorageAccessService storageAccessService, StorageService storageService,
            UserService userService, ProjectManager projectManager, HotJobHolder jobHolder, TrashService trashService) {
        this.modelMapper = modelMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.modelConvertor = modelConvertor;
        this.versionConvertor = versionConvertor;
        this.storagePathCoordinator = storagePathCoordinator;
        this.modelManager = modelManager;
        this.storageAccessService = storageAccessService;
        this.storageService = storageService;
        this.userService = userService;
        this.projectManager = projectManager;
        this.jobHolder = jobHolder;
        this.trashService = trashService;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectManager,
                modelManager,
                modelManager
        );
    }

    public PageInfo<ModelVo> listModel(ModelQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<ModelEntity> entities = modelMapper.listModels(projectId, query.getNamePrefix());
        return PageUtil.toPageInfo(entities, modelConvertor::convert);
    }

    @Transactional
    public Boolean deleteModel(ModelQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getModelUrl());
        Trash trash = Trash.builder()
                .projectId(projectManager.getProjectId(query.getProjectUrl()))
                .objectId(bundleManager.getBundleId(bundleUrl))
                .type(Type.MODEL)
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());
        return RemoveManager.create(bundleManager, modelManager)
                .removeBundle(bundleUrl);
    }

    public Boolean recoverModel(String projectUrl, String modelUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    public List<ModelInfoVo> listModelInfo(String project, String name) {

        if (StringUtils.hasText(name)) {
            Long projectId = projectManager.getProjectId(project);
            ModelEntity model = modelMapper.findByName(name, projectId);
            if (model == null) {
                throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL)
                        .tip("Unable to find the model with name " + name), HttpStatus.BAD_REQUEST);
            }
            return listModelInfoOfModel(model);
        }

        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project,
                userService.currentUserDetail().getIdTableKey());
        List<ModelEntity> entities = modelMapper.listModels(projectEntity.getId(), null);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.parallelStream()
                .map(this::listModelInfoOfModel)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<ModelInfoVo> listModelInfoOfModel(ModelEntity model) {
        List<ModelVersionEntity> versions = modelVersionMapper.listVersions(
                model.getId(), null, null);
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        return versions.parallelStream()
                .map(version -> toModelInfoVo(model, version))
                .collect(Collectors.toList());
    }

    public ModelInfoVo getModelInfo(ModelQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getModelUrl());
        Long modelId = bundleManager.getBundleId(bundleUrl);
        ModelEntity model = modelMapper.findModelById(modelId);
        if (model == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL)
                    .tip("Unable to find model " + query.getModelUrl()), HttpStatus.BAD_REQUEST);
        }

        ModelVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(query.getModelVersionUrl())) {
            // find version by versionId
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(bundleUrl, query.getModelVersionUrl()), modelId);
            versionEntity = modelVersionMapper.findVersionById(versionId);
        }
        if (versionEntity == null) {
            // find current version
            versionEntity = modelVersionMapper.getLatestVersion(model.getId());
        }
        if (versionEntity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL)
                    .tip("Unable to find the version of model " + query.getModelUrl()), HttpStatus.BAD_REQUEST);
        }

        return toModelInfoVo(model, versionEntity);
    }

    private ModelInfoVo toModelInfoVo(ModelEntity model,
            ModelVersionEntity version) {

        //Get file list in storage
        try {
            String storagePath = version.getStoragePath();
            List<StorageFileVo> collect = storageService.listStorageFile(storagePath);

            return ModelInfoVo.builder()
                    .id(idConvertor.convert(model.getId()))
                    .name(model.getModelName())
                    .versionAlias(versionAliasConvertor.convert(version.getVersionOrder()))
                    .versionName(version.getVersionName())
                    .versionTag(version.getVersionTag())
                    .versionMeta(version.getVersionMeta())
                    .manifest(version.getManifest())
                    .createdTime(version.getCreatedTime().getTime())
                    .files(collect)
                    .build();

        } catch (IOException e) {
            log.error("list model storage", e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE)
                    .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean modifyModelVersion(String projectUrl, String modelUrl, String versionUrl, ModelVersion version) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                .create(projectUrl, modelUrl, versionUrl));
        ModelVersionEntity entity = ModelVersionEntity.builder()
                .id(versionId)
                .versionTag(version.getTag())
                .build();
        int update = modelVersionMapper.update(entity);
        log.info("Model Version has been modified. ID={}", version.getId());
        return update > 0;
    }


    public Boolean manageVersionTag(String projectUrl, String modelUrl, String versionUrl,
            TagAction tagAction) {
        try {
            return TagManager.create(bundleManager, modelManager)
                    .updateTag(
                            BundleVersionUrl.create(projectUrl, modelUrl, versionUrl),
                            tagAction);
        } catch (TagException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL).tip(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public Boolean revertVersionTo(String projectUrl, String modelUrl, String versionUrl) {
        return RevertManager.create(bundleManager, modelManager)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
    }

    public PageInfo<ModelVersionVo> listModelVersionHistory(ModelVersionQuery query, PageParams pageParams) {
        Long modelId = bundleManager.getBundleId(BundleUrl
                .create(query.getProjectUrl(), query.getModelUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ModelVersionEntity> entities = modelVersionMapper.listVersions(
                modelId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, entity -> {
            ModelVersionVo vo = versionConvertor.convert(entity);
            vo.setSize(storageService.getStorageSize(entity.getStoragePath()));
            return vo;
        });
    }

    public List<ModelVo> findModelByVersionId(List<Long> versionIds) {

        List<ModelVersionEntity> versions = modelVersionMapper.findVersionsByIds(versionIds);

        List<Long> ids = versions.stream()
                .map(ModelVersionEntity::getModelId)
                .collect(Collectors.toList());

        List<ModelEntity> models = modelMapper.findModelsByIds(ids);

        return models.stream()
                .map(modelConvertor::convert)
                .collect(Collectors.toList());
    }


    /**
     * prefix + / + fileName
     */
    static final String FORMATTER_STORAGE_PATH = "%s/%s";

    @Transactional
    public void upload(MultipartFile dsFile,
            ClientModelRequest uploadRequest) {

        long startTime = System.currentTimeMillis();
        log.debug("access received at {}", startTime);
        Long projectId = null;
        ProjectEntity projectEntity = null;
        if (!StrUtil.isEmpty(uploadRequest.getProject())) {
            projectEntity = projectManager.getProject(uploadRequest.getProject());
            projectId = projectEntity.getId();
        }
        ModelEntity entity = modelMapper.findByNameForUpdate(uploadRequest.name(), projectId);
        if (null == entity) {
            //create
            if (projectId == null) {
                projectEntity = projectManager.findByNameOrDefault(uploadRequest.getProject(),
                        userService.currentUserDetail().getIdTableKey());
                projectId = projectEntity.getId();
            }
            entity = ModelEntity.builder().isDeleted(0)
                    .ownerId(getOwner())
                    .projectId(projectId)
                    .modelName(uploadRequest.name())
                    .build();
            modelMapper.addModel(entity);
        }
        log.debug("model checked time use {}", System.currentTimeMillis() - startTime);
        ModelVersionEntity modelVersionEntity = modelVersionMapper.findByNameAndModelId(
                uploadRequest.version(), entity.getId());
        boolean entityExists = null != modelVersionEntity;
        if (entityExists && !uploadRequest.force()) {
            log.debug("model version checked time use {}", System.currentTimeMillis() - startTime);
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL).tip(
                    "model version duplicate" + uploadRequest.version()),
                    HttpStatus.BAD_REQUEST);
        } else if (entityExists && uploadRequest.force()) {
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                    .parallelStream().forEach(job -> {
                        Model model = job.getModel();
                        if (model.getName().equals(uploadRequest.name())
                                && model.getVersion().equals(uploadRequest.version())) {
                            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL).tip(
                                    "job's are running on model version " + uploadRequest.version()
                                            + " you can't force push now"),
                                    HttpStatus.BAD_REQUEST);
                        }
                    });
        }
        log.debug("model version checked time use {}", System.currentTimeMillis() - startTime);
        //upload to storage
        final String modelPath = entityExists ? modelVersionEntity.getStoragePath()
                : storagePathCoordinator.allocateModelPath(projectEntity.getProjectName(), uploadRequest.name(),
                        uploadRequest.version());
        String jobContent = "";
        try (final InputStream inputStream = dsFile.getInputStream()) {
            // only extract the eval job file content
            jobContent = new String(
                    Objects.requireNonNull(
                            TarFileUtil.getContentFromTarFile(dsFile.getInputStream(), "src", "eval_jobs.yaml")));
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH, modelPath, dsFile.getOriginalFilename()),
                    inputStream, dsFile.getSize());
        } catch (IOException e) {
            log.error("upload model failed {}", uploadRequest.getSwmp(), e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (entityExists) {
            // update job content
            modelVersionEntity.setEvalJobs(jobContent);
            modelVersionMapper.update(modelVersionEntity);
        } else {
            // create new entity
            modelVersionEntity = ModelVersionEntity.builder()
                    .ownerId(getOwner())
                    .storagePath(modelPath)
                    .modelId(entity.getId())
                    .versionName(uploadRequest.version())
                    .versionMeta(uploadRequest.getSwmp())
                    .manifest(uploadRequest.getManifest())
                    .evalJobs(jobContent)
                    .build();
            modelVersionMapper.addNewVersion(modelVersionEntity);
            modelVersionMapper.revertTo(modelVersionEntity.getModelId(), modelVersionEntity.getId());
        }

    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if (null == currentUserDetail) {
            throw new SwAuthException(SwAuthException.AuthType.MODEL_UPLOAD);
        }
        return currentUserDetail.getIdTableKey();
    }

    public void pull(String projectUrl, String modelUrl, String versionUrl, HttpServletResponse httpResponse) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ModelEntity modelEntity = modelMapper.findByName(modelUrl, projectId);
        if (null == modelEntity) {
            throw new SwValidationException(ValidSubject.MODEL).tip("model not found");
        }
        ModelVersionEntity modelVersionEntity = modelVersionMapper.findByNameAndModelId(
                versionUrl, modelEntity.getId());
        if (null == modelVersionEntity) {
            throw new SwValidationException(ValidSubject.MODEL).tip("model version not found");
        }
        List<String> files;
        try {
            files = storageAccessService.list(
                    modelVersionEntity.getStoragePath()).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listing file from storage failed {}", modelVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }
        if (CollectionUtils.isEmpty(files)) {
            throw new SwValidationException(ValidSubject.MODEL).tip("model version empty folder");
        }
        String filePath = files.get(0);
        try (InputStream fileInputStream = storageAccessService.get(filePath);
                ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            String fileName = filePath.substring(modelVersionEntity.getStoragePath().length() + 1);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download file from storage failed {}", modelVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String modelUrl, String versionUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ModelEntity entity = modelMapper.findByName(modelUrl, projectId);
        if (null == entity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL), HttpStatus.NOT_FOUND);
        }
        ModelVersionEntity modelVersionEntity = modelVersionMapper.findByNameAndModelId(versionUrl,
                entity.getId());
        if (null == modelVersionEntity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL), HttpStatus.NOT_FOUND);
        }
        return "";
    }
}
