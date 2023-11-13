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

import static cn.hutool.core.util.BooleanUtil.toInt;

import ai.starwhale.mlops.api.protocol.model.CreateModelVersionRequest;
import ai.starwhale.mlops.api.protocol.model.CreateModelVersionRequest.ModelSourceType;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobRequest;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobResult;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobResult.Status;
import ai.starwhale.mlops.api.protocol.model.ListFilesResult;
import ai.starwhale.mlops.api.protocol.model.ModelInfoVo;
import ai.starwhale.mlops.api.protocol.model.ModelVersionViewVo;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelViewVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.storage.FileNode;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.blob.BlobService;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import ai.starwhale.mlops.domain.ft.FineTuneDomainService;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.ModelPackageStorage.CompressionAlgorithm;
import ai.starwhale.mlops.domain.model.ModelPackageStorage.File;
import ai.starwhale.mlops.domain.model.ModelPackageStorage.FileType;
import ai.starwhale.mlops.domain.model.ModelPackageStorage.MetaBlobIndex;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.domain.model.converter.ModelVersionVoConverter;
import ai.starwhale.mlops.domain.model.converter.ModelVoConverter;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionViewEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
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
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Slf4j
@Service
public class ModelService {

    private static final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();
    private static final LZ4SafeDecompressor lz4SafeDecompressor = lz4Factory.safeDecompressor();

    private final ModelMapper modelMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;
    private final ModelVoConverter modelVoConverter;
    private final ModelVersionVoConverter versionConvertor;
    private final UserService userService;
    private final ProjectService projectService;
    private final ModelDao modelDao;
    private final HotJobHolder jobHolder;
    private final TrashService trashService;
    private final JobSpecParser jobSpecParser;
    private final BlobService blobService;
    private final BundleVersionTagDao bundleVersionTagDao;
    @Setter
    private BundleManager bundleManager;

    private final FineTuneDomainService fineTuneService;

    public ModelService(
            ModelMapper modelMapper,
            ModelVersionMapper modelVersionMapper,
            BundleVersionTagDao bundleVersionTagDao,
            IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor,
            ModelVoConverter modelVoConverter,
            ModelVersionVoConverter versionConvertor,
            ModelDao modelDao,
            UserService userService,
            ProjectService projectService,
            HotJobHolder jobHolder,
            TrashService trashService,
            JobSpecParser jobSpecParser,
            BlobService blobService,
            FineTuneDomainService fineTuneService
    ) {
        this.modelMapper = modelMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.modelVoConverter = modelVoConverter;
        this.versionConvertor = versionConvertor;
        this.modelDao = modelDao;
        this.userService = userService;
        this.projectService = projectService;
        this.jobHolder = jobHolder;
        this.trashService = trashService;
        this.jobSpecParser = jobSpecParser;
        this.fineTuneService = fineTuneService;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectService,
                modelDao,
                modelDao,
                bundleVersionTagDao
        );
        this.blobService = blobService;
        this.bundleVersionTagDao = bundleVersionTagDao;
    }

    public PageInfo<ModelVo> listModel(ModelQuery query, PageParams pageParams) {
        Long projectId = projectService.getProjectId(query.getProjectUrl());
        Long userId = userService.getUserId(query.getOwner());
        try (var pager = PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize())) {
            List<ModelEntity> entities = modelMapper.list(projectId, query.getName(), userId, null);
            return PageUtil.toPageInfo(entities, entity -> {
                ModelVo vo = modelVoConverter.convert(entity);
                var modelVersion = modelVersionMapper.findByLatest(entity.getId());
                if (modelVersion != null) {
                    var tags = bundleVersionTagDao.getTagsByBundleVersions(
                            BundleAccessor.Type.MODEL, entity.getId(), List.of(modelVersion));
                    var versionVo = versionConvertor.convert(
                            modelVersion, modelVersion, tags.get(modelVersion.getId()));
                    versionVo.setOwner(userService.findUserById(modelVersion.getOwnerId()));
                    vo.setVersion(versionVo);
                }
                vo.setOwner(userService.findUserById(entity.getOwnerId()));
                return vo;
            });
        }
    }

    public Model findModel(Long modelId) {
        ModelEntity entity = modelDao.getModel(modelId);
        return Model.fromEntity(entity);
    }

    public ModelVersion findModelVersion(String versionUrl) {
        ModelVersionEntity modelVersion = modelDao.getModelVersion(versionUrl);
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
        List<ModelVersionEntity> versions = modelVersionMapper.list(model.getId(), null, false);
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        return versions.parallelStream()
                .map(version -> toModelInfoVo(model, version))
                .collect(Collectors.toList());
    }

    public Map<String, List<FileNode>> getModelDiff(
            String projectUrl,
            String modelUrl,
            String baseVersion,
            String compareVersion
    ) {
        var baseFiles = this.listModelFiles(projectUrl, modelUrl, baseVersion);
        var compareFiles = this.listModelFiles(projectUrl, modelUrl, compareVersion);
        FileNode.compare(baseFiles, compareFiles);
        return Map.of("baseVersion", baseFiles, "compareVersion", compareFiles);
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
            Long versionId = bundleManager.getBundleVersionId(
                    BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
            versionEntity = modelVersionMapper.find(versionId);
        }
        if (versionEntity == null) {
            // find current version
            versionEntity = modelVersionMapper.findByLatest(model.getId());
        }
        if (versionEntity == null) {
            throw new SwNotFoundException(
                    ResourceType.BUNDLE_VERSION,
                    "Unable to find the version of model " + modelUrl
            );
        }

        return toModelInfoVo(model, versionEntity);
    }

    private ModelInfoVo toModelInfoVo(ModelEntity model, ModelVersionEntity version) {
        var tags = bundleVersionTagDao.getTagsByBundleVersions(
                BundleAccessor.Type.MODEL,
                model.getId(),
                List.of(version)
        );
        return ModelInfoVo.builder()
                .id(idConvertor.convert(model.getId()))
                .name(model.getModelName())
                .versionId(idConvertor.convert(version.getId()))
                .versionAlias(versionAliasConvertor.convert(version.getVersionOrder()))
                .versionName(version.getVersionName())
                .versionTag(version.getVersionTag())
                .createdTime(version.getCreatedTime().getTime())
                .shared(toInt(version.getShared()))
                .versionInfo(versionConvertor.convert(version, version, tags.get(version.getId())))
                .build();
    }

    public Boolean modifyModelVersion(String projectUrl, String modelUrl, String versionUrl, ModelVersion version) {
        if (!StringUtils.hasText(version.getTag()) && !StringUtils.hasText(version.getBuiltInRuntime())) {
            throw new SwValidationException(ValidSubject.MODEL, "no attributes set for model version");
        }
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                                                                  .create(projectUrl, modelUrl, versionUrl));
        ModelVersionEntity entity = ModelVersionEntity.builder()
                .id(versionId)
                .versionTag(version.getTag())
                .builtInRuntime(version.getBuiltInRuntime())
                .build();
        int update = modelVersionMapper.update(entity);
        log.info("Model Version has been modified. ID={}", version.getId());
        return update > 0;
    }

    public Boolean revertVersionTo(String projectUrl, String modelUrl, String versionUrl) {
        return RevertManager.create(bundleManager, modelDao)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
    }

    public PageInfo<ModelVersionVo> listModelVersionHistory(ModelVersionQuery query, PageParams pageParams) {
        Long modelId = bundleManager.getBundleId(BundleUrl.create(query.getProjectUrl(), query.getModelUrl()));

        try (var pager = PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize())) {
            var entities = modelVersionMapper.list(modelId, query.getVersionName(), false);
            var latest = modelVersionMapper.findByLatest(modelId);
            var tags = bundleVersionTagDao.getTagsByBundleVersions(BundleAccessor.Type.MODEL, modelId, entities);
            return PageUtil.toPageInfo(entities, item -> {
                var vo = versionConvertor.convert(item, latest, tags.get(item.getId()));
                vo.setOwner(userService.findUserById(item.getOwnerId()));
                return vo;
            });
        }
    }

    public void shareModelVersion(String projectUrl, String modelUrl, String versionUrl, Boolean shared) {
        // the model can share only in public project
        var project = projectService.getProjectVo(projectUrl);
        if (!project.getPrivacy().equals(Project.Privacy.PUBLIC.name())) {
            throw new SwValidationException(ValidSubject.MODEL, "project is not public");
        }
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                                                                  .create(projectUrl, modelUrl, versionUrl));
        modelVersionMapper.updateShared(versionId, shared);
    }

    public List<ModelViewVo> listModelVersionView(
            String projectUrl, boolean includeShared, boolean includeCurrentProject) {
        var project = projectService.findProject(projectUrl);
        var list = new ArrayList<ModelViewVo>();
        if (includeCurrentProject) {
            var versions = modelVersionMapper.listModelVersionViewByProject(project.getId());
            list.addAll(viewEntityToVo(versions, project));
        }
        if (includeShared) {
            var shared = modelVersionMapper.listModelVersionViewByShared(project.getId());
            list.addAll(viewEntityToVo(shared, project));
        }
        return list;
    }

    public List<ModelViewVo> listRecentlyModelVersionView(String projectUrl, Integer limit) {
        var project = projectService.findProject(projectUrl);
        var userId = userService.currentUserDetail().getId();
        var list = modelVersionMapper.listModelVersionsByUserRecentlyUsed(project.getId(), userId, limit);
        return viewEntityToVo(list, project);
    }

    private List<ModelViewVo> viewEntityToVo(List<ModelVersionViewEntity> list, Project currentProject) {
        Map<Long, ModelViewVo> map = new LinkedHashMap<>();
        var tags = new HashMap<Long, Map<Long, List<String>>>();
        // group by modelId
        var modelGroups = list.stream().collect(Collectors.groupingBy(ModelVersionViewEntity::getModelId));
        for (var entry : modelGroups.entrySet()) {
            var modelId = entry.getKey();
            var entities = entry.getValue();
            var tagsMap = bundleVersionTagDao.getTagsByBundleVersions(BundleAccessor.Type.MODEL, modelId, entities);
            tags.put(modelId, tagsMap);
        }

        Map<Long, Long> latestCache = new HashMap<>();

        for (ModelVersionViewEntity entity : list) {
            if (!map.containsKey(entity.getModelId())) {
                map.put(
                        entity.getModelId(),
                        ModelViewVo.builder()
                                .ownerName(entity.getUserName())
                                .projectName(entity.getProjectName())
                                .modelId(idConvertor.convert(entity.getModelId()))
                                .modelName(entity.getModelName())
                                // TODO: replace by inProject?
                                .shared(!entity.getProjectName().equals(currentProject.getName()))
                                .versions(new ArrayList<>())
                                .build()
                );
            }
            Long latest = latestCache.computeIfAbsent(entity.getId(), value ->
                        modelVersionMapper.findByLatest(entity.getModelId()).getId());
            var versionTags =
                    tags.get(entity.getModelId()) == null ? null : tags.get(entity.getModelId()).get(entity.getId());
            try {
                map.get(entity.getModelId())
                        .getVersions()
                        .add(ModelVersionViewVo.builder()
                                     .id(idConvertor.convert(entity.getId()))
                                     .versionName(entity.getVersionName())
                                     .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                                     .tags(versionTags)
                                     .latest(entity.getId() != null && entity.getId().equals(latest))
                                     .createdTime(entity.getCreatedTime().getTime())
                                     .shared(toInt(entity.getShared()))
                                     .builtInRuntime(entity.getBuiltInRuntime())
                                     .stepSpecs(jobSpecParser.parseAndFlattenStepFromYaml(entity.getJobs()))
                                     .build());
            } catch (JsonProcessingException e) {
                log.error("parse step spec error for model version:{}", entity.getId(), e);
                throw new SwValidationException(ValidSubject.MODEL, e.getMessage());
            }
        }
        return new ArrayList<>(map.values());
    }

    public List<ModelVo> findModelByVersionId(List<Long> versionIds) {
        if (versionIds.isEmpty()) {
            return List.of();
        }
        List<ModelVersionEntity> versions = modelVersionMapper.findByIds(Joiner.on(",").join(versionIds));
        var tags = new HashMap<Long, Map<Long, List<String>>>();
        versions.stream().collect(Collectors.groupingBy(ModelVersionEntity::getModelId))
                .forEach((id, versionList) -> tags.put(
                        id,
                        bundleVersionTagDao.getTagsByBundleVersions(BundleAccessor.Type.RUNTIME, id, versionList)
                ));
        return versions.stream().map(version -> {
            ModelEntity model = modelMapper.find(version.getModelId());
            ModelVersionEntity latest = modelVersionMapper.findByLatest(version.getModelId());
            ModelVo vo = modelVoConverter.convert(model);
            vo.setOwner(userService.findUserById(model.getOwnerId()));
            var versionTags =
                    tags.get(version.getModelId()) == null ? null : tags.get(version.getModelId()).get(version.getId());
            vo.setVersion(versionConvertor.convert(version, latest, versionTags));
            return vo;
        }).collect(Collectors.toList());
    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if (null == currentUserDetail) {
            throw new SwAuthException(SwAuthException.AuthType.MODEL_UPLOAD);
        }
        return currentUserDetail.getId();
    }

    public InitUploadBlobResult initUploadBlob(InitUploadBlobRequest initUploadBlobRequest) {
        try {
            try {
                var blobId = this.blobService.readBlobRef(
                        initUploadBlobRequest.getContentMd5(),
                        initUploadBlobRequest.getContentLength()
                );
                return InitUploadBlobResult.builder()
                        .status(Status.EXISTED)
                        .blobId(blobId)
                        .build();
            } catch (FileNotFoundException e) {
                // if not found, go on
            }

            var blobId = this.blobService.generateBlobId();
            return InitUploadBlobResult.builder()
                    .status(Status.OK)
                    .blobId(blobId)
                    .signedUrl(this.blobService.getSignedPutUrl(blobId))
                    .build();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "", e);
        }
    }

    public String completeUploadBlob(String blobId) {
        return this.blobService.generateBlobRef(blobId);
    }

    @Transactional
    public void createModelVersion(
            String project, String model, String version, CreateModelVersionRequest createModelVersionRequest
    ) {
        var projectEntity = this.projectService.findProject(project);
        if (projectEntity == null) {
            throw new SwNotFoundException(ResourceType.PROJECT, "project not found");
        }
        var ownerId = this.getOwner();
        var modelEntity = this.modelMapper.findByName(model, projectEntity.getId(), true);
        if (modelEntity == null) {
            modelEntity = ModelEntity.builder()
                    .ownerId(ownerId)
                    .projectId(projectEntity.getId())
                    .modelName(model)
                    .build();
            this.modelMapper.insert(modelEntity);
        }
        ModelVersionEntity modelVersionEntity = modelVersionMapper.findByNameAndModelId(
                version, modelEntity.getId());
        if (modelVersionEntity != null) {
            if (createModelVersionRequest.isForce()) {
                jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                        .parallelStream().forEach(job -> {
                            Model jobModel = job.getModel();
                            if (jobModel.getName().equals(model)
                                    && jobModel.getVersion().equals(version)) {
                                throw new StarwhaleApiException(
                                        new SwValidationException(
                                                ValidSubject.MODEL,
                                                "job's are running on model version " + version
                                                        + " you can't force push now"
                                        ),
                                        HttpStatus.CONFLICT
                                );
                            }
                        });
            } else {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.MODEL, "model version duplicate" + version),
                        HttpStatus.CONFLICT
                );
            }
        }
        var metaBlobId = createModelVersionRequest.getMetaBlobId();
        var metaBlob = this.getMetaBlob(metaBlobId);
        var metaBlobList = new ArrayList<ModelPackageStorage.MetaBlob>();
        metaBlobList.add(metaBlob);
        for (var index : metaBlob.getMetaBlobIndexesList()) {
            metaBlobList.add(this.getMetaBlob(index.getBlobId()));
        }
        long storageSize = 0;
        for (var blob : metaBlobList) {
            storageSize += blob.getSerializedSize();
            for (var file : blob.getFilesList()) {
                storageSize += file.getSize();
            }
        }
        String jobs;
        try (var in = this.getFileData(metaBlob, "src/.starwhale/jobs.yaml")) {
            jobs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "read jobs.yaml error", e);
        }
        if (modelVersionEntity == null) {
            boolean sourcedFromFineTune = null != createModelVersionRequest.getModelSource()
                    && createModelVersionRequest.getModelSource().getType() == ModelSourceType.FINE_TUNE
                    && null != createModelVersionRequest.getModelSource().getId();
            modelVersionEntity = ModelVersionEntity.builder()
                    .modelId(modelEntity.getId())
                    .ownerId(ownerId)
                    .versionName(version)
                    .metaBlobId(metaBlobId)
                    .builtInRuntime(createModelVersionRequest.getBuiltInRuntime())
                    .draft(sourcedFromFineTune)
                    .jobs(jobs)
                    .storageSize(storageSize)
                    .build();
            this.modelVersionMapper.insert(modelVersionEntity);
            if (sourcedFromFineTune) {
                fineTuneService.attachTargetModel(
                        createModelVersionRequest.getModelSource().getId(),
                        modelVersionEntity
                );
            }
            RevertManager.create(this.bundleManager, this.modelDao)
                    .revertVersionTo(modelVersionEntity.getModelId(), modelVersionEntity.getId());
        } else {
            modelVersionEntity.setMetaBlobId(metaBlobId);
            modelVersionEntity.setBuiltInRuntime(createModelVersionRequest.getBuiltInRuntime());
            modelVersionEntity.setJobs(jobs);
            modelVersionEntity.setStorageSize(storageSize);
            this.modelVersionMapper.update(modelVersionEntity);
        }
    }

    public ModelPackageStorage.MetaBlob getModelMetaBlob(String project, String model, String version, String blobId) {
        var modelVersionEntity = this.getModelVersion(project, model, version);
        var root = this.getMetaBlob(modelVersionEntity.getMetaBlobId());
        if (blobId.isEmpty()) {
            return this.addSignedUrls(root);
        }
        for (var index : root.getMetaBlobIndexesList()) {
            if (index.getBlobId().equals(blobId)) {
                return this.addSignedUrls(this.getMetaBlob(blobId));
            }
        }
        throw new SwValidationException(ValidSubject.MODEL, "blob " + blobId + " not found");
    }

    public List<FileNode> listModelFiles(String project, String model, String version) {
        return this.listModelFiles(this.getModelMetaBlob(project, model, version, ""));
    }

    private List<FileNode> listModelFiles(ModelPackageStorage.MetaBlob metaBlob) {
        var files = new ArrayList<>(metaBlob.getFilesList());
        for (var index : metaBlob.getMetaBlobIndexesList()) {
            files.addAll(this.getMetaBlob(index.getBlobId()).getFilesList());
        }
        return this.convertRecursively(files, 0).getFiles();
    }

    public ListFilesResult listFiles(String project, String model, String version, String path) {
        return ListFilesResult.builder()
                .files(this.getFile(this.getModelMetaBlob(project, model, version, ""), path).stream()
                               .map(this::convert)
                               .collect(Collectors.toList()))
                .build();
    }

    private FileNode convertRecursively(List<ModelPackageStorage.File> fileList, int index) {
        var f = fileList.get(index);
        var ret = this.convert(f);
        if (ret.getType() == FileNode.Type.DIRECTORY) {
            for (int i = f.getFromFileIndex(); i < f.getToFileIndex(); ++i) {
                if (i < index) {
                    throw new SwProcessException(
                            ErrorType.SYSTEM,
                            "invalid file index. current:" + index + " node:" + f
                    );
                }
                ret.getFiles().add(this.convertRecursively(fileList, i));
            }
        }
        return ret;
    }

    private FileNode convert(ModelPackageStorage.File file) {
        if (file.getType() != FileType.FILE_TYPE_DIRECTORY) {
            return FileNode.builder()
                    .name(file.getName())
                    .type(FileNode.Type.FILE)
                    .size(String.valueOf(file.getSize()))
                    .mime(URLConnection.guessContentTypeFromName(file.getName()))
                    .signature(Hex.encodeHexString(file.getMd5().toByteArray(), true))
                    .build();
        }
        return FileNode.builder()
                .name(file.getName())
                .type(FileNode.Type.DIRECTORY)
                .build();
    }

    public LengthAbleInputStream getFileData(String project, String model, String version, String path) {
        return this.getFileData(this.getModelMetaBlob(project, model, version, ""), path);
    }

    private LengthAbleInputStream getFileData(ModelPackageStorage.MetaBlob metaBlob, String path) {
        var files = new LinkedList<>(this.getFile(metaBlob, path));
        var mainFile = files.get(0);
        var size = mainFile.getSize();
        if (mainFile.getType() == FileType.FILE_TYPE_HARDLINK
                || mainFile.getType() == FileType.FILE_TYPE_SYMLINK) {
            String prefix;
            if (mainFile.getType() == FileType.FILE_TYPE_HARDLINK) {
                prefix = "hardlink:";
            } else {
                prefix = "symlink:";
            }
            var content = (prefix + mainFile.getLink()).getBytes(StandardCharsets.UTF_8);
            return new LengthAbleInputStream(new ByteArrayInputStream(content), content.length);
        }
        if (size == 0) {
            return new LengthAbleInputStream(new ByteArrayInputStream(new byte[0]), 0);
        }
        var compressionAlgorithm = files.get(0).getCompressionAlgorithm();
        if (mainFile.getBlobIdsCount() == 0) {
            files.removeFirst();
        }
        var in = new SequenceInputStream(new Enumeration<>() {
            int index = 0;

            @Override
            public boolean hasMoreElements() {
                return files.size() > 0;
            }

            @Override
            public InputStream nextElement() {
                if (files.isEmpty()) {
                    throw new NoSuchElementException();
                }
                var file = files.getFirst();
                if (index >= file.getBlobIdsCount()) {
                    throw new SwProcessException(
                            ErrorType.SYSTEM,
                            "invalid index. max:" + file.getBlobIdsCount() + " current:" + index
                    );
                }
                var blobId = file.getBlobIds(index);
                ++index;
                if (index == file.getBlobIdsCount()) {
                    files.removeFirst();
                    index = 0;
                }
                if (blobId.isEmpty()) {
                    var array = metaBlob.getData().toByteArray();
                    InputStream in;
                    if (file.getBlobOffset() == 0 && file.getBlobSize() == 0) {
                        in = new ByteArrayInputStream(array);
                    } else {
                        in = new ByteArrayInputStream(array, file.getBlobOffset(), file.getBlobSize());
                    }
                    return readFileData(in, compressionAlgorithm);
                }
                try {
                    var data = blobService.readBlob(blobId, file.getBlobOffset(), file.getBlobSize());
                    return readFileData(data, compressionAlgorithm);
                } catch (IOException e) {
                    throw new SwProcessException(ErrorType.STORAGE, "can not read data", e);
                }
            }
        });
        return new LengthAbleInputStream(in, size);
    }

    private InputStream readFileData(
            InputStream in,
            ModelPackageStorage.CompressionAlgorithm compressionAlgorithm
    ) {
        if (compressionAlgorithm == CompressionAlgorithm.COMPRESSION_ALGORITHM_NO_COMPRESSION) {
            return in;
        }

        var enumInputs = new Enumeration<InputStream>() {
            final byte[] decompressBuf = new byte[65536];
            final byte[] readBuf = new byte[65536];
            InputStream current = null;

            @SneakyThrows
            @Override
            public boolean hasMoreElements() {
                if (current == null) {
                    var high = in.read();
                    if (high < 0) {
                        return false;
                    }
                    var low = in.read();
                    if (low < 0) {
                        throw new SwProcessException(ErrorType.SYSTEM, "failed to read size");
                    }
                    var size = high * 256 + low;
                    if (size == 0) {
                        size = 65536;
                    }
                    for (int i = 0; i < size; ) {
                        int n = in.read(this.readBuf, i, size - i);
                        if (n < 0) {
                            throw new SwProcessException(
                                    ErrorType.SYSTEM,
                                    "not enough data. expected:" + size + " actual:" + i
                            );
                        }
                        i += n;
                    }
                    if (compressionAlgorithm == CompressionAlgorithm.COMPRESSION_ALGORITHM_LZ4) {
                        this.current = new ByteArrayInputStream(this.decompressBuf, 0,
                                                                lz4SafeDecompressor.decompress(
                                                                        this.readBuf,
                                                                        0,
                                                                        size,
                                                                        this.decompressBuf,
                                                                        0
                                                                )
                        );
                    } else {
                        throw new SwProcessException(ErrorType.SYSTEM, "invalid compression:" + compressionAlgorithm);
                    }
                }
                return true;
            }

            @Override
            public InputStream nextElement() {
                var ret = this.current;
                this.current = null;
                return ret;
            }
        };

        return new SequenceInputStream(enumInputs) {
            @Override
            public void close() throws IOException {
                in.close();
            }
        };
    }

    private ModelPackageStorage.MetaBlob addSignedUrls(ModelPackageStorage.MetaBlob metaBlob) {
        var builder = metaBlob.toBuilder();
        for (var file : builder.getFilesBuilderList()) {
            for (var blobId : file.getBlobIdsList()) {
                try {
                    file.addSignedUrls(this.blobService.getSignedUrl(blobId));
                } catch (IOException e) {
                    throw new SwProcessException(ErrorType.STORAGE, "failed to sign url", e);
                }
            }
        }
        return builder.build();
    }

    private ModelPackageStorage.MetaBlob getMetaBlob(String blobId) {
        try {
            var data = this.blobService.readBlobAsByteArray(blobId);
            return ModelPackageStorage.MetaBlob.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.SYSTEM, "failed to parse meta blob", e);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "failed to read blob " + blobId, e);
        }
    }

    // this method is public for testing purpose
    public List<ModelPackageStorage.File> getFile(ModelPackageStorage.MetaBlob firstMetaBlob, String path) {
        var metaBlob = new AtomicReference<>(firstMetaBlob);
        var indexes = new LinkedList<>(metaBlob.get().getMetaBlobIndexesList());
        indexes.addFirst(MetaBlobIndex.newBuilder().setLastFileIndex(firstMetaBlob.getFilesCount() - 1).build());
        var result = new AtomicReference<>(metaBlob.get().getFiles(0));
        if (!StrUtil.isEmpty(path)) {
            var currentPath = new StringBuilder();
            for (var name : path.split("/")) {
                if (currentPath.length() > 0) {
                    currentPath.append("/");
                }
                currentPath.append(name);
                AtomicBoolean found = new AtomicBoolean(false);
                if (result.get().getType() == FileType.FILE_TYPE_DIRECTORY) {
                    this.iterateDir(metaBlob, indexes, result.get(), file -> {
                        if (file.getName().equals(name)) {
                            result.set(file);
                            found.set(true);
                            return true;
                        }
                        return false;
                    });
                }
                if (!found.get()) {
                    throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "file not found: " + currentPath);
                }
            }
        }
        var ret = new ArrayList<ModelPackageStorage.File>();
        switch (result.get().getType()) {
            case FILE_TYPE_DIRECTORY:
                this.iterateDir(metaBlob, indexes, result.get(), file -> {
                    ret.add(file);
                    return false;
                });
                break;
            case FILE_TYPE_HUGE:
                ret.add(result.get());
                this.iterateDir(metaBlob, indexes, result.get(), file -> {
                    ret.add(file);
                    return false;
                });
                break;
            case FILE_TYPE_REGULAR:
            case FILE_TYPE_SYMLINK:
            case FILE_TYPE_HARDLINK:
                ret.add(result.get());
                break;
            default:
                throw new SwProcessException(ErrorType.SYSTEM, "unknown file type " + result.get().getType());
        }
        return ret;
    }

    private void iterateDir(
            AtomicReference<ModelPackageStorage.MetaBlob> metaBlob,
            LinkedList<ModelPackageStorage.MetaBlobIndex> indexes,
            ModelPackageStorage.File dir,
            Function<File, Boolean> processor
    ) {
        var from = dir.getFromFileIndex();
        var to = dir.getToFileIndex();
        for (int i = from; i < to; ++i) {
            if (i > indexes.getFirst().getLastFileIndex()) {
                while (indexes.size() > 0 && i > indexes.getFirst().getLastFileIndex()) {
                    indexes.removeFirst();
                }
                if (indexes.isEmpty()) {
                    throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "can not find file index " + i);
                }
                try {
                    metaBlob.set(ModelPackageStorage.MetaBlob.parseFrom(
                            this.blobService.readBlobAsByteArray(indexes.getFirst().getBlobId())));
                } catch (IOException e) {
                    throw new SwProcessException(ErrorType.STORAGE, "failed to read blob", e);
                }
            }
            var file = metaBlob.get().getFiles(
                    metaBlob.get().getFilesCount() - (indexes.getFirst().getLastFileIndex() - i) - 1);
            if (processor.apply(file)) {
                return;
            }
        }
    }

    public String query(String projectUrl, String modelUrl, String versionUrl) {
        ModelVersionEntity modelVersionEntity = getModelVersion(projectUrl, modelUrl, versionUrl);
        if (null == modelVersionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "Not found.");
        }
        return modelVersionEntity.getName();
    }

    private ModelVersionEntity getModelVersion(String projectUrl, String modelUrl, String versionUrl) {
        Long versionId = bundleManager.getBundleVersionId(
                BundleVersionUrl.create(projectUrl, modelUrl, versionUrl));
        return modelVersionMapper.find(versionId);
    }

    public void addModelVersionTag(String projectUrl, String modelUrl, String versionUrl, String tag, Boolean force) {
        var userId = userService.currentUserDetail().getId();
        bundleManager.addBundleVersionTag(
                BundleAccessor.Type.MODEL,
                projectUrl,
                modelUrl,
                versionUrl,
                tag,
                userId,
                force
        );
    }

    public List<String> listModelVersionTags(String projectUrl, String modelUrl, String versionUrl) {
        return bundleManager.listBundleVersionTags(BundleAccessor.Type.MODEL, projectUrl, modelUrl, versionUrl);
    }

    public void deleteModelVersionTag(String projectUrl, String modelUrl, String versionUrl, String tag) {
        bundleManager.deleteBundleVersionTag(BundleAccessor.Type.MODEL, projectUrl, modelUrl, versionUrl, tag);
    }

    public BundleVersionTagEntity getModelVersionTag(String projectUrl, String modelUrl, String tag) {
        return bundleManager.getBundleVersionTag(BundleAccessor.Type.MODEL, projectUrl, modelUrl, tag);
    }
}
