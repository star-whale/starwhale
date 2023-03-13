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

import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelUploadRequest;
import ai.starwhale.mlops.api.protocol.model.ModelUploadResult;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.storage.FileDesc;
import ai.starwhale.mlops.api.protocol.storage.FileNode;
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
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.storage.MetaInfo;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ModelService {

    static final String MODEL_MANIFEST = "_manifest.yaml";

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
    private final ProjectService projectService;
    private final ModelDao modelDao;
    private final HotJobHolder jobHolder;

    private final TrashService trashService;

    private final YAMLMapper yamlMapper;
    @Setter
    private BundleManager bundleManager;

    public ModelService(ModelMapper modelMapper, ModelVersionMapper modelVersionMapper,
            IdConverter idConvertor, VersionAliasConverter versionAliasConvertor, ModelVoConverter modelVoConverter,
            ModelVersionVoConverter versionConvertor, StoragePathCoordinator storagePathCoordinator,
            ModelDao modelDao, StorageAccessService storageAccessService, StorageService storageService,
            UserService userService, ProjectService projectService, HotJobHolder jobHolder,
            TrashService trashService, YAMLMapper yamlMapper) {
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
        this.projectService = projectService;
        this.jobHolder = jobHolder;
        this.trashService = trashService;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectService,
                modelDao,
                modelDao
        );
        this.yamlMapper = yamlMapper;
    }

    public PageInfo<ModelVo> listModel(ModelQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectService.getProjectId(query.getProjectUrl());
        Long userId = userService.getUserId(query.getOwner());
        List<ModelEntity> entities = modelMapper.list(projectId, query.getNamePrefix(), userId, null);
        return PageUtil.toPageInfo(entities, entity -> {
            ModelVo vo = modelVoConverter.convert(entity);
            vo.setOwner(userService.findUserById(entity.getOwnerId()));
            return vo;
        });
    }

    public Model findModel(Long modelId) {
        ModelEntity entity = modelDao.getModel(modelId);
        return Model.fromEntity(entity);
    }

    public ModelVersion findModelVersion(String versioUrl) {
        ModelVersionEntity modelVersion = modelDao.getModelVersion(versioUrl);
        return ModelVersion.fromEntity(modelVersion);
    }

    public ModelVersion findModelVersion(Long versionId) {
        ModelVersionEntity entity = (ModelVersionEntity) modelDao.findVersionById(versionId);
        return ModelVersion.fromEntity(entity);
    }

    @Transactional
    public Boolean deleteModel(ModelQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getModelUrl());
        Trash trash = Trash.builder()
                .projectId(projectService.getProjectId(query.getProjectUrl()))
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
        Long projectId = projectService.getProjectId(project);
        if (StringUtils.hasText(name)) {
            ModelEntity model = modelMapper.findByName(name, projectId, false);
            if (model == null) {
                throw new SwNotFoundException(ResourceType.BUNDLE, "Unable to find the model with name " + name);
            }
            return listModelInfoOfModel(model);
        }

        List<ModelEntity> entities = modelMapper.list(projectId, null, null, null);
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

    public Map<String, List<FileNode>> getModelDiff(String projectUrl, String modelUrl, String baseVersion,
                                                    String compareVersion) {
        var baseModel = getModelVersion(projectUrl, modelUrl, baseVersion);
        var compareModel = getModelVersion(projectUrl, modelUrl, compareVersion);
        if (Objects.isNull(baseModel) || Objects.isNull(compareModel)) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.MODEL,
                "Unable to find the compare version of model "), HttpStatus.BAD_REQUEST);
        }
        try {
            var baseFiles = parseManifestFiles(getManifest(baseModel));
            var compareFiles = parseManifestFiles(getManifest(compareModel));
            FileNode.compare(baseFiles, compareFiles);
            return Map.of("baseVersion", baseFiles, "compareVersion", compareFiles);
        } catch (JsonProcessingException e) {
            throw new SwValidationException(ValidSubject.MODEL, e.getMessage());
        }
    }

    public ModelInfoVo getModelInfo(ModelQuery query) {
        return getModelInfo(query.getProjectUrl(), query.getModelUrl(), query.getModelVersionUrl());
    }

    public ModelInfoVo getModelInfo(String projectUrl, String modelUrl, String versionUrl) {
        BundleUrl bundleUrl = BundleUrl.create(projectUrl, modelUrl);
        Long modelId = bundleManager.getBundleId(bundleUrl);
        ModelEntity model = modelMapper.find(modelId);
        if (model == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "Unable to find model " + modelUrl);
        }

        ModelVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(versionUrl)) {
            // find version by versionId
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(projectUrl, modelUrl, versionUrl));
            versionEntity = modelVersionMapper.find(versionId);
        }
        if (versionEntity == null) {
            // find current version
            versionEntity = modelVersionMapper.findByLatest(model.getId());
        }
        if (versionEntity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                    "Unable to find the version of model " + modelUrl);
        }

        return toModelInfoVo(model, versionEntity);
    }

    private ModelInfoVo toModelInfoVo(ModelEntity model, ModelVersionEntity version) {
        try {
            // Get file list from manifest
            // TODO read from datastore
            var manifest = getManifest(version);

            return ModelInfoVo.builder()
                    .id(idConvertor.convert(model.getId()))
                    .name(model.getModelName())
                    .versionAlias(versionAliasConvertor.convert(version.getVersionOrder()))
                    .versionName(version.getVersionName())
                    .versionTag(version.getVersionTag())
                    .versionMeta(version.getVersionMeta())
                    .manifest(manifest)
                    .createdTime(version.getCreatedTime().getTime())
                    .files(parseManifestFiles(manifest))
                    .build();

        } catch (IOException e) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE, "list model storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<FileNode> parseManifestFiles(String manifest) throws JsonProcessingException {
        if (StringUtils.hasText(manifest)) {
            var meta = yamlMapper.readValue(manifest, MetaInfo.class);
            return FileNode.makeTree(meta.getResources());
        } else {
            return List.of();
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
            ModelVersionVo vo = versionConvertor.convert(entity, getManifest(entity));
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
    static final String FORMATTER_STORAGE_SRC_FILE_PATH = "%s/src/%s";
    static final String FORMATTER_STORAGE_SRC_PATH = "%s/src";

    @Transactional
    public ModelUploadResult uploadManifest(MultipartFile multipartFile, ModelUploadRequest uploadRequest) {
        long startTime = System.currentTimeMillis();
        log.debug("access received at {}", startTime);
        Long projectId = null;
        Project project = null;
        if (!StrUtil.isEmpty(uploadRequest.getProject())) {
            project = projectService.findProject(uploadRequest.getProject());
            projectId = project.getId();
        }
        ModelEntity entity = modelMapper.findByName(uploadRequest.name(), projectId, true);
        if (null == entity) {
            //create
            if (projectId == null) {
                project = projectService.findProject(uploadRequest.getProject());
                projectId = project.getId();
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
        // upload to storage
        final String modelPackagePath = entityExists ? modelVersionEntity.getStoragePath()
                : storagePathCoordinator.allocateModelPath(projectId, uploadRequest.name(),
                uploadRequest.version());
        String manifestContent;
        Set<String> existed = new HashSet<>();
        try (final InputStream inputStream = multipartFile.getInputStream()) {
            manifestContent = IOUtils.toString(multipartFile.getInputStream(), StandardCharsets.UTF_8);

            // parse model file's signature, valid if existed
            var metaInfo = yamlMapper.readValue(manifestContent, MetaInfo.class);

            for (MetaInfo.Resource file : metaInfo.getResources()) {
                if (!file.isDuplicateCheck()) {
                    continue;
                }
                String modelPath = storagePathCoordinator.allocateCommonModelPoolPath(
                        projectId, file.getSignature());
                var model = storageAccessService.list(modelPath).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(model)) {
                    existed.add(file.getSignature());
                }
            }
            // upload manifest to oss
            // TODO: store to datastore
            storageAccessService.put(
                    String.format(FORMATTER_STORAGE_PATH, modelPackagePath, MODEL_MANIFEST),
                    inputStream, multipartFile.getSize()
            );
        } catch (IOException e) {
            log.error("upload model failed {}", uploadRequest.getSwmp(), e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (entityExists) {
            // update manifest
            modelVersionEntity.setStatus(ModelVersionEntity.STATUS_UN_AVAILABLE);
            modelVersionMapper.update(modelVersionEntity);
        } else {
            // create new entity
            modelVersionEntity = ModelVersionEntity.builder()
                .ownerId(getOwner())
                .storagePath(modelPackagePath)
                .modelId(entity.getId())
                .versionName(uploadRequest.version())
                .versionMeta(uploadRequest.getSwmp())
                .evalJobs("")
                .status(ModelVersionEntity.STATUS_UN_AVAILABLE)
                .build();
            modelVersionMapper.insert(modelVersionEntity);
            RevertManager.create(bundleManager, modelDao)
                    .revertVersionTo(modelVersionEntity.getModelId(), modelVersionEntity.getId());
        }
        return ModelUploadResult.builder()
                .uploadId(modelVersionEntity.getId())
                .existed(existed)
                .build();
    }

    @Transactional
    public void uploadModel(Long modelVersionId, String signature,
                            MultipartFile modelFile, ModelUploadRequest uploadRequest) {
        ModelVersionEntity modelVersionEntity = modelVersionMapper.find(modelVersionId);
        if (modelVersionEntity == null
                || Objects.equals(modelVersionEntity.getStatus(), ModelVersionEntity.STATUS_AVAILABLE)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL),
                    HttpStatus.BAD_REQUEST
            );
        }
        Long projectId = projectService.getProjectId(uploadRequest.getProject());

        String modelPath = storagePathCoordinator.allocateCommonModelPoolPath(
                projectId, signature);

        try {
            storageAccessService.put(modelPath, modelFile.getInputStream());
        } catch (IOException e) {
            log.error("upload model failed {}", uploadRequest.getSwmp(), e);
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Transactional
    public void uploadSrc(Long modelVersionId, MultipartFile multipartFile, ModelUploadRequest uploadRequest) {
        ModelVersionEntity modelVersionEntity = modelVersionMapper.find(modelVersionId);
        if (modelVersionEntity == null
                || Objects.equals(modelVersionEntity.getStatus(), ModelVersionEntity.STATUS_AVAILABLE)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL),
                    HttpStatus.BAD_REQUEST
            );
        }
        //upload to storage
        final String storagePath = modelVersionEntity.getStoragePath();
        String jobContent = "";
        try (final InputStream inputStream = multipartFile.getInputStream()) {
            // only extract the eval job file content
            // TODO: replace with oss path content
            // but update only for job
            jobContent = new String(
                    Objects.requireNonNull(
                            TarFileUtil.getContentFromTarFile(
                                multipartFile.getInputStream(), ".starwhale", "eval_jobs.yaml")
                    )
            );
            TarFileUtil.extract(inputStream, (name, size, in) ->
                    storageAccessService.put(
                            String.format(FORMATTER_STORAGE_SRC_FILE_PATH, storagePath, name), in, size
                    )
            );
        } catch (IOException | ArchiveException e) {
            log.error("upload model src failed {}", uploadRequest.getSwmp(), e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // update job content
        modelVersionEntity.setEvalJobs(jobContent);
        modelVersionMapper.update(modelVersionEntity);
    }

    public void end(Long modelVersionId) {
        modelVersionMapper.updateStatus(modelVersionId, ModelVersionEntity.STATUS_AVAILABLE);
    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if (null == currentUserDetail) {
            throw new SwAuthException(SwAuthException.AuthType.MODEL_UPLOAD);
        }
        return currentUserDetail.getIdTableKey();
    }

    public void pull(FileDesc fileDesc, String name, String path, String signature,
                     String projectUrl, String modelUrl, String versionUrl,
                     HttpServletResponse httpResponse) {
        ModelVersionEntity modelVersionEntity = getModelVersion(projectUrl, modelUrl, versionUrl);
        if (null == modelVersionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "Model version not found");
        }

        if (!StringUtils.hasText(name) && !StringUtils.hasText(path)) {
            throw new SwValidationException(ValidSubject.MODEL,
                "at least one of name or path is not null when download");
        }

        String manifest = getManifest(modelVersionEntity);
        // read from manifest
        try {
            var metaInfo = yamlMapper.readValue(manifest, MetaInfo.class);
            // get file type by path
            for (MetaInfo.Resource file : metaInfo.getResources()) {
                if (file.getPath().equals(path) || file.getName().equals(name)) {
                    fileDesc = file.getDesc();
                    // update correct attributes
                    name = Objects.isNull(name) ? file.getName() : name;
                    path = Objects.isNull(path) ? file.getPath() : path;
                    signature = Objects.isNull(signature) ? file.getSignature() : signature;
                    break;
                }
            }

        } catch (JsonProcessingException e) {
            throw new SwValidationException(ValidSubject.MODEL, "parse manifest error:" + e.getMessage());
        }

        if (fileDesc == null) {
            throw new SwValidationException(ValidSubject.MODEL,
                    String.format("can't find file:%s(path:%s) from model package", name, path));
        }
        String filePath;
        switch (fileDesc) {
            case MANIFEST:
                this.pullFile(
                        name, () -> new ByteArrayInputStream(manifest.getBytes()), httpResponse
                );
                return;
            case SRC:
                filePath = String.format(FORMATTER_STORAGE_PATH, modelVersionEntity.getStoragePath(), path);
                break;
            case MODEL:
                var project = projectService.findProject(projectUrl);
                filePath = storagePathCoordinator.allocateCommonModelPoolPath(project.getId(), signature);
                break;
            case SRC_TAR:
                this.pullSrcTar(name,
                        String.format(FORMATTER_STORAGE_SRC_PATH, modelVersionEntity.getStoragePath()), httpResponse);
                return;
            default:
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.MODEL, "unsupport type " + fileDesc),
                        HttpStatus.BAD_REQUEST
                );
        }
        // direct pull from oss
        this.pullFile(
                name, () -> {
                    try {
                        return storageAccessService.get(filePath);
                    } catch (IOException e) {
                        log.error("get file from storage failed {}", filePath, e);
                        throw new SwProcessException(ErrorType.STORAGE);
                    }
                }, httpResponse);

    }

    private String getManifest(ModelVersionEntity modelVersionEntity) {
        try {
            var p = String.format(FORMATTER_STORAGE_PATH, modelVersionEntity.getStoragePath(), MODEL_MANIFEST);
            var is = storageAccessService.get(p);
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "get manifest error", e);
        }
    }

    public void pullFile(String name, Supplier<InputStream> streamSupplier, HttpServletResponse httpResponse) {
        try (InputStream fileInputStream = streamSupplier.get();
                ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download manifest file failed", e);
            throw new SwProcessException(ErrorType.SYSTEM);
        }
    }

    public void pullSrcTar(String name, String srcPath, HttpServletResponse httpResponse) {
        List<String> files;
        try {
            files = storageAccessService.list(srcPath).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listing file from storage failed {}", srcPath, e);
            throw new SwProcessException(ErrorType.STORAGE);
        }

        if (CollectionUtils.isEmpty(files)) {
            throw new SwValidationException(ValidSubject.MODEL, "model version empty folder");
        }

        try (ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            final long[] length = {0L};
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
                                .name(filePath.substring(srcPath.length() + 1))
                                .build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, outputStream);

            httpResponse.addHeader("Content-Disposition",
                    "attachment; filename=\"" + name + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length[0]));
            outputStream.flush();
        } catch (IOException | ArchiveException e) {
            log.error("download file from storage failed {}", srcPath, e);
            throw new SwProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String modelUrl, String versionUrl) {
        ModelVersionEntity modelVersionEntity = getModelVersion(projectUrl, modelUrl, versionUrl);
        if (null == modelVersionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "Not found.");
        }
        return modelVersionEntity.getName();
    }

    private ModelVersionEntity getModelVersion(
            String projectUrl, String modelUrl, String versionUrl) {
        Long versionId = bundleManager.getBundleVersionId(
                BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
        return modelVersionMapper.find(versionId);
    }
}
