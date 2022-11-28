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
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConverter;
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
import ai.starwhale.mlops.domain.model.converter.ModelVersionVoConverter;
import ai.starwhale.mlops.domain.model.converter.ModelVoConverter;
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
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
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
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;
    private final ModelVoConverter modelVoConverter;
    private final ModelVersionVoConverter versionConvertor;
    private final StoragePathCoordinator storagePathCoordinator;
    private final StorageAccessService storageAccessService;
    private final StorageService storageService;
    private final UserService userService;
    private final ProjectManager projectManager;
    private final ModelDao modelDao;
    private final HotJobHolder jobHolder;

    private final TrashService trashService;
    @Setter
    private BundleManager bundleManager;

    public ModelService(ModelMapper modelMapper, ModelVersionMapper modelVersionMapper,
            IdConverter idConvertor, VersionAliasConverter versionAliasConvertor, ModelVoConverter modelVoConverter,
            ModelVersionVoConverter versionConvertor, StoragePathCoordinator storagePathCoordinator,
            ModelDao modelDao, StorageAccessService storageAccessService, StorageService storageService,
            UserService userService, ProjectManager projectManager, HotJobHolder jobHolder, TrashService trashService) {
        this.modelMapper = modelMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.modelVoConverter = modelVoConverter;
        this.versionConvertor = versionConvertor;
        this.storagePathCoordinator = storagePathCoordinator;
        this.modelDao = modelDao;
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
                modelDao,
                modelDao
        );
    }

    public PageInfo<ModelVo> listModel(ModelQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<ModelEntity> entities = modelMapper.list(projectId, query.getNamePrefix(), null);
        return PageUtil.toPageInfo(entities, entity -> {
            ModelVo vo = modelVoConverter.convert(entity);
            vo.setOwner(userService.findUserById(entity.getOwnerId()));
            return vo;
        });
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
        return RemoveManager.create(bundleManager, modelDao)
                .removeBundle(bundleUrl);
    }

    public Boolean recoverModel(String projectUrl, String modelUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    public List<ModelInfoVo> listModelInfo(String project, String name) {

        if (StringUtils.hasText(name)) {
            Long projectId = projectManager.getProjectId(project);
            ModelEntity model = modelMapper.findByName(name, projectId, false);
            if (model == null) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.MODEL, "Unable to find the model with name " + name),
                        HttpStatus.BAD_REQUEST);
            }
            return listModelInfoOfModel(model);
        }

        ProjectEntity projectEntity = projectManager.getProject(project);
        List<ModelEntity> entities = modelMapper.list(projectEntity.getId(), null, null);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.parallelStream()
                .map(this::listModelInfoOfModel)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<ModelInfoVo> listModelInfoOfModel(ModelEntity model) {
        List<ModelVersionEntity> versions = modelVersionMapper.list(
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
        ModelEntity model = modelMapper.find(modelId);
        if (model == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL, "Unable to find model " + query.getModelUrl()),
                    HttpStatus.BAD_REQUEST);
        }

        ModelVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(query.getModelVersionUrl())) {
            // find version by versionId
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(query.getProjectUrl(), query.getModelUrl(), query.getModelVersionUrl()));
            versionEntity = modelVersionMapper.find(versionId);
        }
        if (versionEntity == null) {
            // find current version
            versionEntity = modelVersionMapper.findByLatest(model.getId());
        }
        if (versionEntity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL,
                    "Unable to find the version of model " + query.getModelUrl()), HttpStatus.BAD_REQUEST);
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
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE, "list model storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
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
            return TagManager.create(bundleManager, modelDao)
                    .updateTag(
                            BundleVersionUrl.create(projectUrl, modelUrl, versionUrl),
                            tagAction);
        } catch (TagException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL, "failed to create tag manager", e),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public Boolean revertVersionTo(String projectUrl, String modelUrl, String versionUrl) {
        return RevertManager.create(bundleManager, modelDao)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
    }

    public PageInfo<ModelVersionVo> listModelVersionHistory(ModelVersionQuery query, PageParams pageParams) {
        Long modelId = bundleManager.getBundleId(BundleUrl
                .create(query.getProjectUrl(), query.getModelUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ModelVersionEntity> entities = modelVersionMapper.list(
                modelId, query.getVersionName(), query.getVersionTag());
        ModelVersionEntity latest = modelVersionMapper.findByLatest(modelId);
        return PageUtil.toPageInfo(entities, entity -> {
            ModelVersionVo vo = versionConvertor.convert(entity);
            if (latest != null && Objects.equals(entity.getId(), latest.getId())) {
                //vo.setTag(TagUtil.addTags("latest", vo.getTag()));
                vo.setAlias(VersionAliasConverter.LATEST);
            }
            vo.setSize(storageService.getStorageSize(entity.getStoragePath()));
            return vo;
        });
    }

    public List<ModelVo> findModelByVersionId(List<Long> versionIds) {

        List<ModelVersionEntity> versions = modelVersionMapper.findByIds(Joiner.on(",").join(versionIds));

        List<Long> ids = versions.stream()
                .map(ModelVersionEntity::getModelId)
                .collect(Collectors.toList());

        List<ModelEntity> models = modelMapper.findByIds(Joiner.on(",").join(ids));

        return models.stream()
                .map(model -> {
                    ModelVo vo = modelVoConverter.convert(model);
                    vo.setOwner(userService.findUserById(model.getOwnerId()));
                    return vo;
                }).collect(Collectors.toList());
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
        ModelEntity entity = modelMapper.findByName(uploadRequest.name(), projectId, true);
        if (null == entity) {
            //create
            if (projectId == null) {
                projectEntity = projectManager.getProject(uploadRequest.getProject());
                projectId = projectEntity.getId();
            }
            entity = ModelEntity.builder().isDeleted(0)
                    .ownerId(getOwner())
                    .projectId(projectId)
                    .modelName(uploadRequest.name())
                    .build();
            modelMapper.insert(entity);
        }
        log.debug("model checked time use {}", System.currentTimeMillis() - startTime);
        ModelVersionEntity modelVersionEntity = modelVersionMapper.findByNameAndModelId(
                uploadRequest.version(), entity.getId());
        boolean entityExists = null != modelVersionEntity;
        if (entityExists && !uploadRequest.force()) {
            log.debug("model version checked time use {}", System.currentTimeMillis() - startTime);
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL, "model version duplicate" + uploadRequest.version()),
                    HttpStatus.BAD_REQUEST);
        } else if (entityExists && uploadRequest.force()) {
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                    .parallelStream().forEach(job -> {
                        Model model = job.getModel();
                        if (model.getName().equals(uploadRequest.name())
                                && model.getVersion().equals(uploadRequest.version())) {
                            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL,
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
            // TODO: replace with oss path content
            // but update only for job
            jobContent = new String(
                    Objects.requireNonNull(
                            TarFileUtil.getContentFromTarFile(dsFile.getInputStream(), "src", "eval_jobs.yaml")));
            TarFileUtil.extract(inputStream, (name, size, in) ->
                    storageAccessService.put(
                            String.format(FORMATTER_STORAGE_PATH, modelPath, name), in, size
                    )
            );
        } catch (IOException | ArchiveException e) {
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
            modelVersionMapper.insert(modelVersionEntity);
            RevertManager.create(bundleManager, modelDao).revertVersionTo(BundleVersionUrl.create(
                    idConvertor.convert(projectId),
                    idConvertor.convert(modelVersionEntity.getModelId()),
                    idConvertor.convert(modelVersionEntity.getId())));
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
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
        ModelVersionEntity modelVersionEntity = modelVersionMapper.find(versionId);
        if (null == modelVersionEntity) {
            throw new SwValidationException(ValidSubject.MODEL, "model version not found");
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
            throw new SwValidationException(ValidSubject.MODEL, "model version empty folder");
        }

        try (ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            final long[] length = {0L};
            if (files.size() == 1 && files.get(0).endsWith(".swmp")) {
                var filePath = files.get(0);
                try (LengthAbleInputStream fileInputStream = storageAccessService.get(filePath)) {
                    length[0] += fileInputStream.transferTo(outputStream);
                }
            } else {
                TarFileUtil.archiveAndTransferTo(new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return !files.isEmpty();
                    }

                    @Override
                    public TarFileUtil.TarEntry next() {
                        var filePath = files.remove(0);
                        try {
                            var inputStream = storageAccessService.get(filePath);
                            length[0] += inputStream.getSize();
                            return TarFileUtil.TarEntry.builder()
                                    .inputStream(inputStream)
                                    .size(inputStream.getSize())
                                    .name(filePath.substring(modelVersionEntity.getStoragePath().length() + 1))
                                    .build();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, outputStream);
            }

            httpResponse.addHeader("Content-Disposition",
                    "attachment; filename=\"" + modelVersionEntity.getVersionName() + "\".swmp");
            httpResponse.addHeader("Content-Length", String.valueOf(length[0]));
            outputStream.flush();
        } catch (IOException | ArchiveException e) {
            log.error("download file from storage failed {}", modelVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String modelUrl, String versionUrl) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
        ModelVersionEntity modelVersionEntity = modelVersionMapper.find(versionId);
        if (null == modelVersionEntity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL), HttpStatus.NOT_FOUND);
        }
        return "";
    }
}
