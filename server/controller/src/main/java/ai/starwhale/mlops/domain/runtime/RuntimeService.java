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

package ai.starwhale.mlops.domain.runtime;

import static cn.hutool.core.util.BooleanUtil.toInt;

import ai.starwhale.mlops.api.protocol.runtime.BuildImageResult;
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionViewVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeViewVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.storage.FlattenFileVo;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.RunEnvs;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.VirtualJobLoader;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.bo.Runtime;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeConverter;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeVersionConverter;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionViewEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
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
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class RuntimeService {

    static final String RUNTIME_MANIFEST = "_manifest.yaml";
    private final RuntimeMapper runtimeMapper;
    private final RuntimeVersionMapper runtimeVersionMapper;
    private final StorageService storageService;
    private final ProjectService projectService;
    private final RuntimeConverter runtimeConvertor;
    private final RuntimeVersionConverter versionConvertor;
    private final RuntimeDao runtimeDao;
    private final StoragePathCoordinator storagePathCoordinator;
    private final StorageAccessService storageAccessService;
    private final UserService userService;
    private final IdConverter idConvertor;
    private final HotJobHolder jobHolder;
    private final VersionAliasConverter versionAliasConvertor;
    private final TrashService trashService;
    @Setter
    private BundleManager bundleManager;
    private final DockerSetting dockerSetting;
    private final RunTimeProperties runTimeProperties;
    private final BundleVersionTagDao bundleVersionTagDao;

    private final JobCreator jobCreator;

    private final VirtualJobLoader virtualJobLoader;

    private final JobSpecParser jobSpecParser;

    public RuntimeService(
            RuntimeMapper runtimeMapper,
            RuntimeVersionMapper runtimeVersionMapper,
            BundleVersionTagDao bundleVersionTagDao,
            StorageService storageService,
            ProjectService projectService,
            RuntimeConverter runtimeConvertor,
            RuntimeVersionConverter versionConvertor,
            RuntimeDao runtimeDao,
            StoragePathCoordinator storagePathCoordinator,
            StorageAccessService storageAccessService,
            HotJobHolder jobHolder,
            UserService userService,
            IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor,
            TrashService trashService,
            DockerSetting dockerSetting,
            RunTimeProperties runTimeProperties,
            JobCreator jobCreator, VirtualJobLoader virtualJobLoader, JobSpecParser jobSpecParser) {
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.bundleVersionTagDao = bundleVersionTagDao;
        this.storageService = storageService;
        this.projectService = projectService;
        this.runtimeConvertor = runtimeConvertor;
        this.versionConvertor = versionConvertor;
        this.runtimeDao = runtimeDao;
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.jobHolder = jobHolder;
        this.userService = userService;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.trashService = trashService;
        this.dockerSetting = dockerSetting;
        this.runTimeProperties = runTimeProperties;
        this.jobCreator = jobCreator;
        this.virtualJobLoader = virtualJobLoader;
        this.jobSpecParser = jobSpecParser;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectService,
                runtimeDao,
                runtimeDao,
                bundleVersionTagDao);
    }

    public PageInfo<RuntimeVo> listRuntime(RuntimeQuery runtimeQuery, PageParams pageParams) {
        Long projectId = projectService.getProjectId(runtimeQuery.getProjectUrl());
        Long userId = userService.getUserId(runtimeQuery.getOwner());
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<RuntimeEntity> entities = runtimeMapper.list(projectId, runtimeQuery.getNamePrefix(), userId, null);

        return PageUtil.toPageInfo(entities, rt -> {
            RuntimeVo vo = runtimeConvertor.convert(rt);
            RuntimeVersionEntity version = runtimeVersionMapper.findByLatest(rt.getId());
            if (version != null) {
                var tags = bundleVersionTagDao.getTagsByBundleVersions(
                        BundleAccessor.Type.RUNTIME, rt.getId(), List.of(version));
                RuntimeVersionVo versionVo = versionConvertor.convert(version, version, tags.get(version.getId()));
                versionVo.setOwner(userService.findUserById(version.getOwnerId()));
                vo.setVersion(versionVo);
            }
            vo.setOwner(userService.findUserById(rt.getOwnerId()));
            return vo;
        });
    }

    public List<RuntimeViewVo> listRuntimeVersionView(String projectUrl) {
        Long projectId = projectService.getProjectId(projectUrl);
        var versions = runtimeVersionMapper.listRuntimeVersionViewByProject(projectId);
        var shared = runtimeVersionMapper.listRuntimeVersionViewByShared(projectId);
        var list = new ArrayList<>(viewEntityToVo(versions, false));
        list.addAll(viewEntityToVo(shared, true));
        return list;
    }

    private Collection<RuntimeViewVo> viewEntityToVo(List<RuntimeVersionViewEntity> list, Boolean shared) {
        Map<Long, RuntimeViewVo> map = new LinkedHashMap<>();
        for (RuntimeVersionViewEntity entity : list) {
            if (!map.containsKey(entity.getRuntimeId())) {
                map.put(
                        entity.getRuntimeId(),
                        RuntimeViewVo.builder()
                                .ownerName(entity.getUserName())
                                .projectName(entity.getProjectName())
                                .runtimeId(idConvertor.convert(entity.getRuntimeId()))
                                .runtimeName(entity.getRuntimeName())
                                .shared(toInt(shared))
                                .versions(new ArrayList<>())
                                .build());
            }
            RuntimeVersionEntity latest = runtimeVersionMapper.findByLatest(entity.getRuntimeId());
            map.get(entity.getRuntimeId())
                    .getVersions()
                    .add(RuntimeVersionViewVo.builder()
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

    public Runtime findRuntime(Long runtimeId) {
        RuntimeEntity entity = runtimeDao.getRuntime(runtimeId);
        return Runtime.fromEntity(entity);
    }

    public RuntimeVersion findRuntimeVersion(String versionUrl) {
        RuntimeVersionEntity entity = runtimeDao.getRuntimeVersion(versionUrl);
        return RuntimeVersion.fromEntity(entity);
    }

    public RuntimeVersion findRuntimeVersion(Long versionId) {
        RuntimeVersionEntity entity = (RuntimeVersionEntity) runtimeDao.findVersionById(versionId);
        return RuntimeVersion.fromEntity(entity);
    }

    public Boolean deleteRuntime(RuntimeQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getRuntimeUrl());
        Trash trash = Trash.builder()
                .projectId(projectService.getProjectId(query.getProjectUrl()))
                .objectId(bundleManager.getBundleId(bundleUrl))
                .type(Type.RUNTIME)
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());
        return RemoveManager.create(bundleManager, runtimeDao)
                .removeBundle(BundleUrl.create(query.getProjectUrl(), query.getRuntimeUrl()));
    }

    public RuntimeInfoVo getRuntimeInfo(RuntimeQuery runtimeQuery) {
        BundleUrl bundleUrl = BundleUrl.create(runtimeQuery.getProjectUrl(), runtimeQuery.getRuntimeUrl());
        Long runtimeId = bundleManager.getBundleId(bundleUrl);
        RuntimeEntity rt = runtimeMapper.find(runtimeId);
        if (rt == null) {
            throw new SwNotFoundException(
                    ResourceType.BUNDLE,
                    "Unable to find runtime " + runtimeQuery.getRuntimeUrl());
        }

        RuntimeVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(runtimeQuery.getRuntimeVersionUrl())) {
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(
                            runtimeQuery.getProjectUrl(),
                            runtimeQuery.getRuntimeUrl(),
                            runtimeQuery.getRuntimeVersionUrl()));
            versionEntity = runtimeVersionMapper.find(versionId);
        }
        if (versionEntity == null) {
            versionEntity = runtimeVersionMapper.findByLatest(rt.getId());
        }
        if (versionEntity == null) {
            throw new SwNotFoundException(
                    ResourceType.BUNDLE_VERSION,
                    "Unable to find the version of runtime " + runtimeQuery.getRuntimeUrl());
        }

        return toRuntimeInfoVo(rt, versionEntity);
    }

    private RuntimeInfoVo toRuntimeInfoVo(RuntimeEntity rt, RuntimeVersionEntity versionEntity) {
        try {
            String storagePath = versionEntity.getStoragePath();
            List<FlattenFileVo> collect = storageService.listStorageFile(storagePath);
            var tags = bundleVersionTagDao.getTagsByBundleVersions(
                    BundleAccessor.Type.RUNTIME, rt.getId(), List.of(versionEntity));

            return RuntimeInfoVo.builder()
                    .id(idConvertor.convert(rt.getId()))
                    .name(rt.getRuntimeName())
                    .versionId(idConvertor.convert(versionEntity.getId()))
                    .versionAlias(versionAliasConvertor.convert(versionEntity.getVersionOrder()))
                    .versionName(versionEntity.getVersionName())
                    .versionTag(versionEntity.getVersionTag())
                    .versionMeta(versionEntity.getVersionMeta())
                    .shared(toInt(versionEntity.getShared()))
                    .createdTime(versionEntity.getCreatedTime().getTime())
                    .versionInfo(versionConvertor.convert(
                            versionEntity,
                            versionEntity,
                            tags.get(versionEntity.getId())))
                    .files(collect)
                    .build();

        } catch (IOException e) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.STORAGE, "list runtime storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean modifyRuntimeVersion(
            String projectUrl,
            String runtimeUrl,
            String runtimeVersionUrl,
            RuntimeVersion version) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                .create(projectUrl, runtimeUrl, runtimeVersionUrl));
        String tag = version.getVersionTag();
        RuntimeVersionEntity entity = RuntimeVersionEntity.builder()
                .id(versionId)
                .versionTag(tag)
                .build();

        int update = runtimeVersionMapper.update(entity);
        log.info("Runtime Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public void shareRuntimeVersion(String projectUrl, String runtimeUrl, String runtimeVersionUrl, Boolean shared) {
        var project = projectService.getProjectVo(projectUrl);
        if (!project.getPrivacy().equals(Project.Privacy.PUBLIC.name())) {
            throw new SwValidationException(ValidSubject.RUNTIME, "project is not public");
        }
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                .create(projectUrl, runtimeUrl, runtimeVersionUrl));
        runtimeVersionMapper.updateShared(versionId, shared);
    }

    public Boolean revertVersionTo(String projectUrl, String runtimeUrl, String runtimeVersionUrl) {
        return RevertManager.create(bundleManager, runtimeDao)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, runtimeUrl, runtimeVersionUrl));
    }

    public PageInfo<RuntimeVersionVo> listRuntimeVersionHistory(RuntimeVersionQuery query, PageParams pageParams) {
        Long runtimeId = bundleManager.getBundleId(BundleUrl.create(query.getProjectUrl(), query.getRuntimeUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        var entities = runtimeVersionMapper.list(runtimeId, query.getVersionName());
        var latest = runtimeVersionMapper.findByLatest(runtimeId);
        var tags = bundleVersionTagDao.getTagsByBundleVersions(BundleAccessor.Type.RUNTIME, runtimeId, entities);
        return PageUtil.toPageInfo(entities, entity -> {
            RuntimeVersionVo vo = versionConvertor.convert(entity, latest, tags.get(entity.getId()));
            vo.setOwner(userService.findUserById(entity.getOwnerId()));
            return vo;
        });
    }

    public List<RuntimeVo> findRuntimeByVersionIds(List<Long> versionIds) {
        if (versionIds.isEmpty()) {
            return List.of();
        }
        var versions = runtimeVersionMapper.findByIds(Joiner.on(",").join(versionIds));

        var tags = new HashMap<Long, List<String>>();
        versions.stream().collect(Collectors.groupingBy(RuntimeVersionEntity::getRuntimeId))
                .forEach((id, versionList) -> tags.putAll(
                        bundleVersionTagDao.getTagsByBundleVersions(
                                BundleAccessor.Type.RUNTIME, id, versionList)));

        return versions.stream().map(version -> {
            RuntimeEntity rt = runtimeMapper.find(version.getRuntimeId());
            RuntimeVersionEntity latest = runtimeVersionMapper.findByLatest(version.getRuntimeId());
            RuntimeVo vo = runtimeConvertor.convert(rt);
            vo.setVersion(versionConvertor.convert(version, latest, tags.get(version.getId())));
            vo.setOwner(userService.findUserById(version.getOwnerId()));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<RuntimeInfoVo> listRuntimeInfo(String project, String name) {
        if (StringUtils.hasText(name)) {
            Long projectId = projectService.getProjectId(project);
            RuntimeEntity rt = runtimeMapper.findByName(name, projectId, false);
            if (rt == null) {
                throw new SwNotFoundException(ResourceType.BUNDLE, "Unable to find the runtime with name " + name);
            }
            return runtimeInfoOfRuntime(rt);
        }

        Long projectId = projectService.getProjectId(project);
        List<RuntimeEntity> runtimeEntities = runtimeMapper.list(projectId, null, null, null);
        if (runtimeEntities == null || runtimeEntities.isEmpty()) {
            return List.of();
        }

        return runtimeEntities.parallelStream()
                .map(this::runtimeInfoOfRuntime)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<RuntimeInfoVo> runtimeInfoOfRuntime(RuntimeEntity rt) {
        List<RuntimeVersionEntity> runtimeVersionEntities = runtimeVersionMapper.list(rt.getId(), null);

        if (runtimeVersionEntities == null || runtimeVersionEntities.isEmpty()) {
            return List.of();
        }

        return runtimeVersionEntities.parallelStream()
                .map(entity -> toRuntimeInfoVo(rt, entity)).collect(Collectors.toList());
    }

    static final String FORMATTER_STORAGE_PATH = "%s/%s";

    @Transactional
    public void upload(MultipartFile dsFile, ClientRuntimeRequest uploadRequest) {

        long startTime = System.currentTimeMillis();
        log.debug("access received at {}", startTime);
        Project project = projectService.findProject(uploadRequest.getProject());
        Long projectId = project.getId();
        RuntimeEntity entity = runtimeMapper.findByName(uploadRequest.name(), projectId, true);
        if (null == entity) {
            // create
            entity = RuntimeEntity.builder().isDeleted(0)
                    .ownerId(userService.currentUserDetail().getId())
                    .projectId(projectId)
                    .runtimeName(uploadRequest.name())
                    .build();
            runtimeMapper.insert(entity);
        }
        log.debug("Runtime checked time use {}", System.currentTimeMillis() - startTime);
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.findByNameAndRuntimeId(
                uploadRequest.version(),
                entity.getId());
        boolean entityExists = (null != runtimeVersionEntity);
        if (entityExists && !uploadRequest.force()) {
            log.debug("Runtime version checked time use {}", System.currentTimeMillis() - startTime);
            throw new StarwhaleApiException(
                    new SwValidationException(
                            ValidSubject.RUNTIME,
                            "Runtime version duplicate" + uploadRequest.version()),
                    HttpStatus.CONFLICT);
        } else if (entityExists && uploadRequest.force()) {
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                    .parallelStream().forEach(job -> {
                        JobRuntime runtime = job.getJobRuntime();
                        if (runtime.getName().equals(uploadRequest.name()) && runtime.getVersion()
                                .equals(uploadRequest.version())) {
                            throw new StarwhaleApiException(
                                    new SwValidationException(
                                            ValidSubject.RUNTIME,
                                            "job's are running on runtime version " + uploadRequest.version()
                                                    + " you can't force push now"),
                                    HttpStatus.BAD_REQUEST);
                        }
                    });
        }
        log.debug("Runtime version checked time use {}", System.currentTimeMillis() - startTime);
        // upload to storage
        final String runtimePath = entityExists ? runtimeVersionEntity.getStoragePath()
                : storagePathCoordinator.allocateRuntimePath(project.getId(), uploadRequest.name(),
                        uploadRequest.version());

        try (final InputStream inputStream = dsFile.getInputStream()) {
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH, runtimePath, dsFile.getOriginalFilename()),
                    inputStream, dsFile.getSize());
        } catch (IOException e) {
            log.error("upload runtime failed {}", uploadRequest.getRuntime(), e);
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        /* create new entity */
        if (!entityExists) {
            String runtimeManifest;
            try (final InputStream inputStream = dsFile.getInputStream()) {
                // extract the manifest file content
                runtimeManifest = new String(
                        Objects.requireNonNull(
                                TarFileUtil.getContentFromTarFile(inputStream, "", RUNTIME_MANIFEST)));
            } catch (IOException e) {
                log.error("upload runtime failed {}", uploadRequest.getRuntime(), e);
                throw new StarwhaleApiException(
                        new SwProcessException(ErrorType.SYSTEM),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            RuntimeVersionEntity version = RuntimeVersionEntity.builder()
                    .ownerId(userService.currentUserDetail().getId())
                    .storagePath(runtimePath)
                    .runtimeId(entity.getId())
                    .versionName(uploadRequest.version())
                    .versionMeta(runtimeManifest)
                    .build();
            runtimeVersionMapper.insert(version);
            RevertManager.create(bundleManager, runtimeDao)
                    .revertVersionTo(version.getRuntimeId(), version.getId());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RuntimeManifest {

        @JsonProperty("base_image")
        String baseImage;

        Environment environment;

        Docker docker;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Environment {

            String python;
            Lock lock;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Lock {

            @JsonProperty("starwhale_version")
            String swVersion;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Docker {

            @JsonProperty("builtin_run_image")
            BuiltinImage builtinImage;

            @JsonProperty("custom_run_image")
            String customImage;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class BuiltinImage {

            @JsonProperty("fullname")
            String fullName;
            String name;
            String repo;
            String tag;
        }
    }

    public void pull(String projectUrl, String runtimeUrl, String versionUrl, HttpServletResponse httpResponse) {
        RuntimeVersionEntity runtimeVersionEntity = (RuntimeVersionEntity) bundleManager.getBundleVersion(
                BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl));
        if (null == runtimeVersionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "Runtime version not found");
        }
        List<String> files;
        try {
            files = storageAccessService.list(
                    runtimeVersionEntity.getStoragePath()).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listing runtime version files from storage failed {}", runtimeVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }
        if (CollectionUtils.isEmpty(files)) {
            throw new SwValidationException(ValidSubject.RUNTIME, "Runtime version empty folder");
        }
        String filePath = files.get(0);
        try (InputStream fileInputStream = storageAccessService.get(
                filePath); ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            String fileName = filePath.substring(runtimeVersionEntity.getStoragePath().length() + 1);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download runtime file from storage failed {}", runtimeVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String runtimeUrl, String versionUrl) {
        RuntimeVersionEntity runtimeVersion = (RuntimeVersionEntity) bundleManager.getBundleVersion(
                BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl));
        if (null == runtimeVersion) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "Not found.");
        }
        return runtimeVersion.getName();
    }

    public Boolean recoverRuntime(String projectUrl, String runtimeUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    public BuildImageResult dockerize(String projectUrl, String runtimeUrl, String versionUrl, RunEnvs runEnvs) {
        var runtimeVersion = (RuntimeVersionEntity) bundleManager.getBundleVersion(
                BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl));
        if (null == runtimeVersion) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "Not found.");
        }

        // TODO: search image name in all records' built_image
        var builtImage = runtimeVersion.getBuiltImage();
        if (StringUtils.hasText(builtImage)) {
            return BuildImageResult.builder()
                    .success(false)
                    .message(String.format("Runtime image [%s] has already existed", builtImage))
                    .build();
        }

        if (!validateDockerSetting(dockerSetting)) {
            throw new SwValidationException(
                    ValidSubject.RUNTIME,
                    "can't found docker registry info, please set it in system setting.");
        }

        List<StepSpec> stepSpecs;
        try {
            String rtDockerizedSteps = virtualJobLoader.loadJobStepSpecs("runtime_dockerizing");
            stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(rtDockerizedSteps);
        } catch (JsonProcessingException e) {
            throw new SwValidationException(
                    ValidSubject.SETTING,
                    "runtime dockerized spec not valid in your $SW_JOB_VIRTUAL_SPECS_PATH");
        } catch (IOException e) {
            throw new SwValidationException(
                    ValidSubject.SETTING,
                    "runtime dockerized spec not found in your $SW_JOB_VIRTUAL_SPECS_PATH");
        }
        if (CollectionUtils.isEmpty(stepSpecs)) {
            throw new SwValidationException(
                    ValidSubject.SETTING,
                    "runtime dockerized spec is empty in your $SW_JOB_VIRTUAL_SPECS_PATH");
        }
        var runtime = bundleManager.getBundle(BundleUrl.create(projectUrl, runtimeUrl));
        var targetImage = new DockerImage(
                dockerSetting.getRegistryForPull(),
                // repository can only contain the characters abcdefghijklmnopqrstuvwxyz0123456789_-./
                String.format("%s:%s", runtime.getName().toLowerCase(), runtimeVersion.getVersionName()));
        var destinationImage = new DockerImage(
                dockerSetting.getRegistryForPush(),
                String.format("%s:%s", runtime.getName().toLowerCase(), runtimeVersion.getVersionName()));
        stepSpecs.forEach(stepSpec -> {
            List<Env> env = stepSpec.getEnv();
            if (null == env) {
                env = new ArrayList<>();
            }
            if (null != runEnvs && null != runEnvs.getEnvVars()) {
                List<Env> collect = runEnvs.getEnvVars().entrySet().stream().map(entry -> new Env(
                        entry.getKey(),
                        entry.getValue()))
                        .collect(Collectors.toList());
                env.addAll(collect);
            }
            env.add(new Env("SW_TARGET_IMAGE", targetImage.toString()));
            env.add(new Env("SW_DEST_IMAGE", destinationImage.toString()));
            env.add(new Env(
                    "SW_RUNTIME_VERSION",
                    String.format("%s/version/%s", runtime.getName(), runtimeVersion.getVersionName())));
            stepSpec.setEnv(env);
        });
        String stepSpecOverWrites;
        try {
            stepSpecOverWrites = Constants.yamlMapper.writeValueAsString(stepSpecs);
        } catch (JsonProcessingException e) {
            throw new SwProcessException(ErrorType.SYSTEM, "error occurs while writing ds build step specs to string",
                    e);
        }
        var project = projectService.findProject(projectUrl);
        Job job = jobCreator.createJob(project,
                null,
                null,
                null,
                "runtime-dockerizing",
                runTimeProperties.getImageBuild().getResourcePool(),
                null,
                stepSpecOverWrites,
                JobType.BUILT_IN,
                null,
                false,
                null,
                null,
                userService.currentUserDetail());
        return new BuildImageResult(true, job.getId().toString());

    }

    private boolean validateDockerSetting(DockerSetting setting) {
        return null != setting
                && StringUtils.hasText(setting.getRegistryForPull())
                && StringUtils.hasText(setting.getRegistryForPush());
    }

    @Transactional
    public void updateImage(
            String projectUrl,
            String runtimeUrl,
            String versionUrl,
            String runtimeImage) {
        if (!StringUtils.hasText(runtimeImage)) {
            return;
        }
        RuntimeVersionEntity runtimeVersionEntity = (RuntimeVersionEntity) bundleManager.getBundleVersion(
                BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl));
        if (null == runtimeVersionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "runtime version not found");
        }
        runtimeDao.updateVersionBuiltImage(runtimeVersionEntity.getVersionName(), runtimeImage);
    }

    public void addRuntimeVersionTag(
            String projectUrl,
            String runtimeUrl,
            String versionUrl,
            String tag,
            Boolean force) {
        var userId = userService.currentUserDetail().getId();
        bundleManager.addBundleVersionTag(
                BundleAccessor.Type.RUNTIME,
                projectUrl,
                runtimeUrl,
                versionUrl,
                tag,
                userId,
                force);
    }

    public List<String> listRuntimeVersionTags(String projectUrl, String runtimeUrl, String versionUrl) {
        return bundleManager.listBundleVersionTags(BundleAccessor.Type.RUNTIME, projectUrl, runtimeUrl, versionUrl);
    }

    public void deleteRuntimeVersionTag(String projectUrl, String runtimeUrl, String versionUrl, String tag) {
        bundleManager.deleteBundleVersionTag(BundleAccessor.Type.RUNTIME, projectUrl, runtimeUrl, versionUrl, tag);
    }

    public BundleVersionTagEntity getRuntimeVersionTag(String projectUrl, String runtimeUrl, String tag) {
        return bundleManager.getBundleVersionTag(BundleAccessor.Type.RUNTIME, projectUrl, runtimeUrl, tag);
    }
}
