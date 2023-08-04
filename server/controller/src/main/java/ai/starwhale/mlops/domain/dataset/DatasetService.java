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

package ai.starwhale.mlops.domain.dataset;

import static ai.starwhale.mlops.schedule.k8s.ResourceOverwriteSpec.RESOURCE_CPU;
import static ai.starwhale.mlops.schedule.k8s.ResourceOverwriteSpec.RESOURCE_MEMORY;
import static cn.hutool.core.util.BooleanUtil.toInt;

import ai.starwhale.mlops.api.protocol.dataset.DatasetInfoVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionViewVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetViewVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.dataset.build.BuildRecordVo;
import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.api.protocol.storage.FlattenFileVo;
import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.configuration.security.DatasetBuildTokenValidator;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersionQuery;
import ai.starwhale.mlops.domain.dataset.build.BuildStatus;
import ai.starwhale.mlops.domain.dataset.build.bo.CreateBuildRecordRequest;
import ai.starwhale.mlops.domain.dataset.build.mapper.BuildRecordMapper;
import ai.starwhale.mlops.domain.dataset.build.po.BuildRecordEntity;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVersionVoConverter;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVoConverter;
import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.dataset.dataloader.DataReadRequest;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionViewEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.storage.UriAccessor;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.schedule.k8s.ContainerOverwriteSpec;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import ai.starwhale.mlops.schedule.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.k8s.ResourceOverwriteSpec;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DatasetService {

    private final DatasetMapper datasetMapper;
    private final DatasetVersionMapper datasetVersionMapper;
    private final BuildRecordMapper buildRecordMapper;
    private final DatasetVoConverter datasetVoConverter;
    private final DatasetVersionVoConverter versionConvertor;
    private final StorageService storageService;
    private final StorageAccessService storageAccessService;
    private final ProjectService projectService;
    private final DatasetDao datasetDao;
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;
    private final UserService userService;
    private final UriAccessor uriAccessor;
    private final DataLoader dataLoader;
    private final TrashService trashService;
    private final K8sClient k8sClient;
    private final K8sJobTemplate k8sJobTemplate;
    private final DatasetBuildTokenValidator datasetBuildTokenValidator;
    private final SystemSettingService systemSettingService;
    private final String instanceUri;
    private final BundleVersionTagDao bundleVersionTagDao;
    @Setter
    private BundleManager bundleManager;

    public DatasetService(
            ProjectService projectService,
            DatasetMapper datasetMapper,
            DatasetVersionMapper datasetVersionMapper,
            BundleVersionTagDao bundleVersionTagDao,
            BuildRecordMapper buildRecordMapper,
            DatasetVoConverter datasetVoConverter,
            DatasetVersionVoConverter versionConvertor,
            StorageService storageService,
            StorageAccessService storageAccessService,
            DatasetDao datasetDao,
            IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor,
            UserService userService,
            UriAccessor uriAccessor,
            DataLoader dataLoader,
            TrashService trashService,
            K8sClient k8sClient,
            K8sJobTemplate k8sJobTemplate,
            DatasetBuildTokenValidator datasetBuildTokenValidator,
            SystemSettingService systemSettingService,
            @Value("${sw.instance-uri}") String instanceUri
    ) {
        this.projectService = projectService;
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.bundleVersionTagDao = bundleVersionTagDao;
        this.buildRecordMapper = buildRecordMapper;
        this.datasetVoConverter = datasetVoConverter;
        this.versionConvertor = versionConvertor;
        this.storageService = storageService;
        this.storageAccessService = storageAccessService;
        this.datasetDao = datasetDao;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.userService = userService;
        this.uriAccessor = uriAccessor;
        this.dataLoader = dataLoader;
        this.trashService = trashService;
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
        this.datasetBuildTokenValidator = datasetBuildTokenValidator;
        this.systemSettingService = systemSettingService;
        this.instanceUri = instanceUri;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectService,
                datasetDao,
                datasetDao,
                bundleVersionTagDao
        );
    }


    public PageInfo<DatasetVo> listDataset(DatasetQuery query, PageParams pageParams) {
        Long projectId = projectService.getProjectId(query.getProjectUrl());
        Long userId = userService.getUserId(query.getOwner());
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<DatasetEntity> entities = datasetMapper.list(projectId, query.getNamePrefix(), userId, null);

        return PageUtil.toPageInfo(entities, ds -> {
            DatasetVo vo = datasetVoConverter.convert(ds);
            DatasetVersionEntity version = datasetVersionMapper.findByLatest(ds.getId());
            if (version != null) {
                var tags = bundleVersionTagDao.getTagsByBundleVersions(
                        BundleAccessor.Type.DATASET, ds.getId(), List.of(version));
                vo.setVersion(versionConvertor.convert(version, version, tags.get(version.getId())));
            }
            return vo;
        });
    }

    public List<DatasetViewVo> listDatasetVersionView(String projectUrl) {
        Long projectId = projectService.getProjectId(projectUrl);
        var versions = datasetVersionMapper.listDatasetVersionViewByProject(projectId);
        var shared = datasetVersionMapper.listDatasetVersionViewByShared(projectId);
        var list = new ArrayList<>(viewEntityToVo(versions, false));
        list.addAll(viewEntityToVo(shared, true));
        return list;
    }

    private Collection<DatasetViewVo> viewEntityToVo(List<DatasetVersionViewEntity> list, Boolean shared) {
        Map<Long, DatasetViewVo> map = new LinkedHashMap<>();
        for (DatasetVersionViewEntity entity : list) {
            if (!map.containsKey(entity.getDatasetId())) {
                map.put(
                        entity.getDatasetId(),
                        DatasetViewVo.builder()
                                .ownerName(entity.getUserName())
                                .projectName(entity.getProjectName())
                                .datasetId(idConvertor.convert(entity.getDatasetId()))
                                .datasetName(entity.getDatasetName())
                                .shared(toInt(shared))
                                .versions(new ArrayList<>())
                                .build()
                );
            }
            DatasetVersionEntity latest = datasetVersionMapper.findByLatest(entity.getDatasetId());
            map.get(entity.getDatasetId())
                    .getVersions()
                    .add(DatasetVersionViewVo.builder()
                                 .id(idConvertor.convert(entity.getId()))
                                 .versionName(entity.getVersionName())
                                 .alias(versionAliasConvertor.convert(entity.getVersionOrder()))
                                 .latest(entity.getId() != null && entity.getId().equals(latest.getId()))
                                 .createdTime(entity.getCreatedTime().getTime())
                                 .shared(toInt(entity.getShared()))
                                 .build());
        }
        return map.values();
    }

    public Boolean deleteDataset(DatasetQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl());
        Trash trash = Trash.builder()
                .projectId(projectService.getProjectId(query.getProjectUrl()))
                .objectId(bundleManager.getBundleId(bundleUrl))
                .type(Type.DATASET)
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());
        return RemoveManager.create(bundleManager, datasetDao)
                .removeBundle(BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl()));
    }

    public Boolean recoverDataset(String projectUrl, String datasetUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    public DatasetVersion findDatasetVersion(String versionUrl) {
        return datasetDao.getDatasetVersion(versionUrl);
    }

    public DatasetInfoVo getDatasetInfo(DatasetQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl());
        Long datasetId = bundleManager.getBundleId(bundleUrl);
        DatasetEntity ds = datasetMapper.find(datasetId);
        if (ds == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "Unable to find dataset " + query.getDatasetUrl());
        }

        DatasetVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(query.getDatasetVersionUrl())) {
            Long versionId = bundleManager.getBundleVersionId(datasetId, query.getDatasetVersionUrl());
            versionEntity = datasetVersionMapper.find(versionId);
        }
        if (versionEntity == null) {
            versionEntity = datasetVersionMapper.findByLatest(ds.getId());
        }
        if (versionEntity == null) {
            throw new SwNotFoundException(
                    ResourceType.BUNDLE_VERSION,
                    "Unable to find the latest version of dataset " + query.getDatasetUrl()
            );
        }
        return toDatasetInfoVo(ds, versionEntity);

    }

    private DatasetInfoVo toDatasetInfoVo(DatasetEntity ds, DatasetVersionEntity versionEntity) {

        //Get file list in storage
        try {
            String storagePath = versionEntity.getStoragePath();
            List<FlattenFileVo> collect = storageService.listStorageFile(storagePath);
            return DatasetInfoVo.builder()
                    .id(idConvertor.convert(ds.getId()))
                    .name(ds.getDatasetName())
                    .versionId(idConvertor.convert(versionEntity.getId()))
                    .versionName(versionEntity.getVersionName())
                    .versionAlias(versionAliasConvertor.convert(versionEntity.getVersionOrder()))
                    .versionTag(versionEntity.getVersionTag())
                    .versionMeta(versionEntity.getVersionMeta())
                    .createdTime(versionEntity.getCreatedTime().getTime())
                    .indexTable(versionEntity.getIndexTable())
                    .shared(toInt(versionEntity.getShared()))
                    .files(collect)
                    .build();

        } catch (IOException e) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.STORAGE, "list dataset storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public void shareDatasetVersion(String projectUrl, String datasetUrl, String versionUrl, Boolean shared) {
        var project = projectService.getProjectVo(projectUrl);
        if (!project.getPrivacy().equals(Project.Privacy.PUBLIC.name())) {
            throw new SwValidationException(ValidSubject.DATASET, "project is not public");
        }
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(projectUrl, datasetUrl, versionUrl));
        datasetVersionMapper.updateShared(versionId, shared);
    }

    public Boolean revertVersionTo(String projectUrl, String datasetUrl, String versionUrl) {
        return RevertManager.create(bundleManager, datasetDao)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, datasetUrl, versionUrl));
    }

    public PageInfo<DatasetVersionVo> listDatasetVersionHistory(DatasetVersionQuery query, PageParams pageParams) {
        Long datasetId = bundleManager.getBundleId(BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        var entities = datasetVersionMapper.list(datasetId, query.getVersionName());
        var latest = datasetVersionMapper.findByLatest(datasetId);
        var tags = bundleVersionTagDao.getTagsByBundleVersions(BundleAccessor.Type.DATASET, datasetId, entities);
        return PageUtil.toPageInfo(entities, item -> versionConvertor.convert(item, latest, tags.get(item.getId())));
    }

    public List<DatasetVo> findDatasetsByVersionIds(List<Long> versionIds) {
        if (versionIds.isEmpty()) {
            return List.of();
        }
        List<DatasetVersionEntity> versions = datasetVersionMapper.findByIds(Joiner.on(",").join(versionIds));

        var tags = new HashMap<Long, List<String>>();
        // group by dataset id and then query by the dataset id and dataset version id list
        versions.stream().collect(Collectors.groupingBy(DatasetVersionEntity::getDatasetId))
                .forEach((datasetId, versionList) -> tags.putAll(
                        bundleVersionTagDao.getTagsByBundleVersions(
                                BundleAccessor.Type.DATASET, datasetId, versionList)));

        return versions.stream().map(version -> {
            DatasetEntity ds = datasetMapper.find(version.getDatasetId());
            DatasetVersionEntity latest = datasetVersionMapper.findByLatest(version.getDatasetId());
            DatasetVo vo = datasetVoConverter.convert(ds);
            vo.setVersion(versionConvertor.convert(version, latest, tags.get(version.getId())));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<DatasetInfoVo> listDs(String project, String name) {
        if (StringUtils.hasText(name)) {
            Long projectId = projectService.getProjectId(project);
            DatasetEntity ds = datasetMapper.findByName(name, projectId, false);
            if (null == ds) {
                throw new SwNotFoundException(ResourceType.BUNDLE, "Unable to find the dataset with name " + name);
            }
            return swDatasetInfoOfDs(ds);
        }
        Long projectId = projectService.getProjectId(project);

        List<DatasetEntity> swDatasetEntities = datasetMapper.list(projectId, null, null, null);
        if (null == swDatasetEntities || swDatasetEntities.isEmpty()) {
            return List.of();
        }
        return swDatasetEntities.parallelStream()
                .map(this::swDatasetInfoOfDs)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<DatasetInfoVo> swDatasetInfoOfDs(DatasetEntity ds) {
        List<DatasetVersionEntity> versionEntities = datasetVersionMapper.list(ds.getId(), null);
        if (null == versionEntities || versionEntities.isEmpty()) {
            return List.of();
        }
        return versionEntities.parallelStream()
                .map(entity -> toDatasetInfoVo(ds, entity)).collect(Collectors.toList());
    }

    public DatasetVersion query(String projectUrl, String datasetUrl, String versionUrl) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                                                                  .create(projectUrl, datasetUrl, versionUrl));
        return datasetDao.getDatasetVersion(versionId);
    }


    public DataIndexDesc nextData(DataReadRequest request) {
        var dataRange = dataLoader.next(request);
        return Objects.isNull(dataRange) ? null : DataIndexDesc.builder()
                .start(dataRange.getStart())
                .end(dataRange.getEnd())
                .build();
    }

    public byte[] dataOf(
            String project, String datasetName, String uri, Long offset,
            Long size
    ) {
        return uriAccessor.dataOf(projectService.findProject(project).getId(), datasetName, uri, offset, size);
    }

    public String signLink(String project, String datasetName, String uri, Long expTimeMillis) {
        return uriAccessor.linkOf(projectService.findProject(project).getId(), datasetName, uri, expTimeMillis);
    }

    public Map<String, String> signLinks(String project, String datasetName, Set<String> uris, Long expTimeMillis) {
        return uris.stream().collect(Collectors.toMap(u -> u, u -> {
            try {
                return signLink(project, datasetName, u, expTimeMillis);
            } catch (Exception e) {
                return "";
            }
        }));
    }

    @Transactional
    public void build(CreateBuildRecordRequest request) {
        var project = projectService.findProject(request.getProjectUrl());
        if (request.getDatasetId() == null || request.getDatasetId() == 0) {
            // create new dataset
            // check the dataset name whether already in the same project
            var ds = datasetMapper.findByName(request.getDatasetName(), project.getId(), true);
            if (null != ds) {
                throw new SwValidationException(ValidSubject.DATASET, MessageFormat.format(
                        "The dataset:{0} in project:{1} is already exists, please use another name.",
                        request.getDatasetName(), project.getName()
                ));
            }
        } else {
            var ds = datasetMapper.find(request.getDatasetId());
            if (null == ds) {
                throw new SwValidationException(ValidSubject.DATASET, MessageFormat.format(
                        "The dataset:{0} in project:{1} doesn't exists, please check it.",
                        request.getDatasetName(), project.getName()
                ));
            }
            if (!ds.getDatasetName().equals(request.getDatasetName())) {
                throw new SwValidationException(ValidSubject.DATASET, MessageFormat.format(
                        "The dataset:{0} is different with the exist dataset:{1}.",
                        request.getDatasetName(), ds.getDatasetName()
                ));
            }
        }
        var buildings = buildRecordMapper.selectBuildingInOneProjectForUpdate(
                project.getId(), request.getDatasetName());
        if (buildings.size() > 0) {
            throw new SwValidationException(ValidSubject.DATASET, MessageFormat.format(
                    "The dataset:{0} in project:{1} is already in building.",
                    request.getDatasetName(), project.getName()
            ));
        }
        var entity = BuildRecordEntity.builder()
                .datasetId(request.getDatasetId())
                .projectId(project.getId())
                .shared(request.getShared())
                .datasetName(request.getDatasetName())
                .storagePath(request.getStoragePath())
                .type(request.getType())
                .status(BuildStatus.CREATED)
                .createdTime(new Date())
                .build();
        var res = buildRecordMapper.insert(entity) > 0;
        if (res) {
            // start build
            var user = userService.currentUserDetail();
            DockerImage image = null;
            String swVersion = "";
            String pyVersion = "";
            String rp = null;
            var runConfig = systemSettingService.getRunTimeProperties();
            if (runConfig != null && runConfig.getDatasetBuild() != null) {
                rp = runConfig.getDatasetBuild().getResourcePool();
                if (StringUtils.hasText(runConfig.getDatasetBuild().getImage())) {
                    image = new DockerImage(runConfig.getDatasetBuild().getImage());
                }
                if (StringUtils.hasText(runConfig.getDatasetBuild().getClientVersion())) {
                    swVersion = runConfig.getDatasetBuild().getClientVersion();
                }
                if (StringUtils.hasText(runConfig.getDatasetBuild().getPythonVersion())) {
                    pyVersion = runConfig.getDatasetBuild().getPythonVersion();
                }
            }
            if (null == image || !StringUtils.hasText(swVersion) || !StringUtils.hasText(pyVersion)) {
                throw new SwValidationException(ValidSubject.DATASET, "Please config dataset build setting!");
            }
            var pool = Objects.isNull(rp) ? null : systemSettingService.queryResourcePool(rp);

            var job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_DATASET_BUILD);

            // record id to annotations
            var info = Map.of("id", String.valueOf(entity.getId()));
            k8sJobTemplate.updateAnnotations(job.getMetadata(), info);
            k8sJobTemplate.updateAnnotations(job.getSpec().getTemplate().getMetadata(), info);

            Map<String, ContainerOverwriteSpec> ret = new HashMap<>();
            var envVars = List.of(
                    new V1EnvVar().name("SW_VERSION").value(swVersion),
                    new V1EnvVar().name("SW_RUNTIME_PYTHON_VERSION").value(pyVersion),
                    new V1EnvVar().name("SW_PYPI_INDEX_URL").value(runConfig.getPypi().getIndexUrl()),
                    new V1EnvVar().name("SW_PYPI_EXTRA_INDEX_URL").value(runConfig.getPypi().getExtraIndexUrl()),
                    new V1EnvVar().name("SW_PYPI_TRUSTED_HOST").value(runConfig.getPypi().getTrustedHost()),
                    new V1EnvVar().name("SW_PYPI_TIMEOUT").value(String.valueOf(runConfig.getPypi().getTimeout())),
                    new V1EnvVar().name("SW_PYPI_RETRIES").value(String.valueOf(runConfig.getPypi().getRetries())),
                    new V1EnvVar().name("SW_INSTANCE_URI").value(instanceUri),
                    new V1EnvVar().name("SW_PROJECT").value(project.getName()),
                    new V1EnvVar().name("SW_TOKEN").value(datasetBuildTokenValidator.getToken(user, entity.getId())),
                    new V1EnvVar().name("DATASET_BUILD_NAME").value(entity.getDatasetName()),
                    new V1EnvVar().name("DATASET_BUILD_TYPE").value(String.valueOf(entity.getType())),
                    new V1EnvVar().name("DATASET_BUILD_DIR_PREFIX").value(entity.getStoragePath())
            );
            DockerImage finalImage = image;
            k8sJobTemplate.getContainersTemplates(job).forEach(templateContainer -> {
                ContainerOverwriteSpec containerOverwriteSpec = new ContainerOverwriteSpec(templateContainer.getName());
                containerOverwriteSpec.setEnvs(envVars);
                containerOverwriteSpec.setImage(
                        finalImage.resolve(systemSettingService.getDockerSetting().getRegistryForPull()));
                containerOverwriteSpec.setCmds(List.of("dataset_build"));
                List<RuntimeResource> resources = Objects.isNull(pool) ? List.of()
                        : pool.validateAndPatchResource(List.of(
                        RuntimeResource.builder().type(RESOURCE_CPU).build(),
                        RuntimeResource.builder().type(RESOURCE_MEMORY).build()
                ));
                log.info("using resource pool {}, patched resources {}", pool, resources);
                containerOverwriteSpec.setResourceOverwriteSpec(new ResourceOverwriteSpec(resources));

                ret.put(templateContainer.getName(), containerOverwriteSpec);
            });
            Map<String, String> nodeSelector = pool != null ? pool.getNodeSelector() : Map.of();
            var toleration = pool != null ? pool.getTolerations() : null;
            k8sJobTemplate.renderJob(job, String.format("%s-%d", entity.getDatasetName(), entity.getId()),
                                     "Never", 0, ret, nodeSelector, toleration, null
            );

            log.debug("deploying dataset build job to k8s :{}", JSONUtil.toJsonStr(job));
            try {
                k8sClient.deployJob(job);
            } catch (ApiException e) {
                throw new SwProcessException(ErrorType.K8S, "deploy dataset build job failed", e);
            }
        } else {
            throw new SwProcessException(ErrorType.DB, "create build record failed");
        }
    }

    @Transactional
    public boolean updateBuildStatus(Long id, BuildStatus status) {
        var record = buildRecordMapper.selectById(id);
        if (record == null) {
            log.warn("build record:{} can't find when update status to {}.", id, status);
            return false;
        }
        var res = buildRecordMapper.updateStatus(id, status) > 0;
        if (res && status == BuildStatus.SUCCESS && record.getShared()) {
            // update shared
            var dataset = datasetMapper.findByName(record.getDatasetName(), record.getProjectId(), false);
            if (dataset != null) {
                var version = datasetVersionMapper.findByLatest(dataset.getId());
                if (version != null) {
                    datasetVersionMapper.updateShared(version.getId(), true);
                }
            }
        }
        return res;
    }

    public PageInfo<BuildRecordVo> listBuildRecords(String projectUrl, BuildStatus status, PageParams pageParams) {
        var project = projectService.findProject(projectUrl);
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        var entities = buildRecordMapper.selectByStatus(project.getId(), status);
        return PageUtil.toPageInfo(entities, entity -> BuildRecordVo.builder()
                .id(String.valueOf(entity.getId()))
                .datasetId(String.valueOf(entity.getDatasetId()))
                .datasetName(entity.getDatasetName())
                .type(entity.getType())
                .status(entity.getStatus())
                .createTime(entity.getCreatedTime().getTime())
                .build()
        );
    }

    public String buildLogContent(Long id) {
        var record = buildRecordMapper.selectById(id);
        if (StringUtils.hasText(record.getLogPath())) {
            try (InputStream inputStream = storageAccessService.get(record.getLogPath())) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new SwProcessException(ErrorType.DB,
                                             MessageFormat.format("read build log path failed {}", id), e
                );
            }
        }
        return "";
    }

    public void addDatasetVersionTag(
            String projectUrl,
            String datasetUrl,
            String versionUrl,
            String tag,
            Boolean force
    ) {
        var userId = userService.currentUserDetail().getId();
        bundleManager.addBundleVersionTag(
                BundleAccessor.Type.DATASET,
                projectUrl,
                datasetUrl,
                versionUrl,
                tag,
                userId,
                force
        );
    }

    public List<String> listDatasetVersionTags(String projectUrl, String datasetUrl, String versionUrl) {
        return bundleManager.listBundleVersionTags(BundleAccessor.Type.DATASET, projectUrl, datasetUrl, versionUrl);
    }

    public void deleteDatasetVersionTag(String projectUrl, String datasetUrl, String versionUrl, String tag) {
        bundleManager.deleteBundleVersionTag(BundleAccessor.Type.DATASET, projectUrl, datasetUrl, versionUrl, tag);
    }

    public BundleVersionTagEntity getDatasetVersionTag(String projectUrl, String datasetUrl, String tag) {
        return bundleManager.getBundleVersionTag(BundleAccessor.Type.RUNTIME, projectUrl, datasetUrl, tag);
    }
}
