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

import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.storage.FlattenFileVo;
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
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeConverter;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeVersionConverter;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class RuntimeService {

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
    private final ObjectMapper yamlMapper;
    private final TrashService trashService;
    @Setter
    private BundleManager bundleManager;

    public RuntimeService(RuntimeMapper runtimeMapper, RuntimeVersionMapper runtimeVersionMapper,
            StorageService storageService, ProjectService projectService,
            @Qualifier("yamlMapper") ObjectMapper yamlMapper, RuntimeConverter runtimeConvertor,
            RuntimeVersionConverter versionConvertor, RuntimeDao runtimeDao,
            StoragePathCoordinator storagePathCoordinator, StorageAccessService storageAccessService,
            HotJobHolder jobHolder, UserService userService, IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor, TrashService trashService) {
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.storageService = storageService;
        this.projectService = projectService;
        this.yamlMapper = yamlMapper;
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
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectService,
                runtimeDao,
                runtimeDao
        );
    }

    public PageInfo<RuntimeVo> listRuntime(RuntimeQuery runtimeQuery, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectService.getProjectId(runtimeQuery.getProjectUrl());
        Long userId = userService.getUserId(runtimeQuery.getOwner());
        List<RuntimeEntity> entities = runtimeMapper.list(projectId, runtimeQuery.getNamePrefix(), userId, null);

        return PageUtil.toPageInfo(entities, rt -> {
            RuntimeVo vo = runtimeConvertor.convert(rt);
            RuntimeVersionEntity version = runtimeVersionMapper.findByLatest(rt.getId());
            if (version != null) {
                RuntimeVersionVo versionVo = versionConvertor.convert(version);
                versionVo.setOwner(userService.findUserById(version.getOwnerId()));
                vo.setVersion(versionVo);
            }
            vo.setOwner(userService.findUserById(rt.getOwnerId()));
            return vo;
        });
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
            throw new SwNotFoundException(ResourceType.BUNDLE,
                "Unable to find runtime " + runtimeQuery.getRuntimeUrl());
        }

        RuntimeVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(runtimeQuery.getRuntimeVersionUrl())) {
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(runtimeQuery.getProjectUrl(), runtimeQuery.getRuntimeUrl(),
                            runtimeQuery.getRuntimeVersionUrl()));
            versionEntity = runtimeVersionMapper.find(versionId);
        }
        if (versionEntity == null) {
            versionEntity = runtimeVersionMapper.findByLatest(rt.getId());
        }
        if (versionEntity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                "Unable to find the version of runtime " + runtimeQuery.getRuntimeUrl());
        }

        return toRuntimeInfoVo(rt, versionEntity);
    }

    private RuntimeInfoVo toRuntimeInfoVo(RuntimeEntity rt, RuntimeVersionEntity versionEntity) {
        try {
            String storagePath = versionEntity.getStoragePath();
            List<FlattenFileVo> collect = storageService.listStorageFile(storagePath);

            return RuntimeInfoVo.builder()
                    .id(idConvertor.convert(rt.getId()))
                    .name(rt.getRuntimeName())
                    .versionAlias(versionAliasConvertor.convert(versionEntity.getVersionOrder()))
                    .versionName(versionEntity.getVersionName())
                    .versionTag(versionEntity.getVersionTag())
                    .versionMeta(versionEntity.getVersionMeta())
                    .createdTime(versionEntity.getCreatedTime().getTime())
                    .files(collect)
                    .build();

        } catch (IOException e) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE, "list runtime storage", e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean modifyRuntimeVersion(String projectUrl, String runtimeUrl, String runtimeVersionUrl,
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

    public Boolean manageVersionTag(String projectUrl, String runtimeUrl, String versionUrl,
            TagAction tagAction) {
        try {
            return TagManager.create(bundleManager, runtimeDao)
                    .updateTag(BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl), tagAction);
        } catch (TagException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.RUNTIME, "failed to creat tag manager", e),
                    HttpStatus.BAD_REQUEST);
        }

    }

    public Boolean revertVersionTo(String projectUrl, String runtimeUrl, String runtimeVersionUrl) {
        return RevertManager.create(bundleManager, runtimeDao)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, runtimeUrl, runtimeVersionUrl));
    }

    public PageInfo<RuntimeVersionVo> listRuntimeVersionHistory(RuntimeVersionQuery query, PageParams pageParams) {
        Long runtimeId = bundleManager.getBundleId(BundleUrl.create(query.getProjectUrl(), query.getRuntimeUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<RuntimeVersionEntity> entities = runtimeVersionMapper.list(
                runtimeId, query.getVersionName(), query.getVersionTag());
        RuntimeVersionEntity latest = runtimeVersionMapper.findByLatest(runtimeId);
        return PageUtil.toPageInfo(entities, entity -> {
            RuntimeVersionVo vo = versionConvertor.convert(entity);
            if (latest != null && Objects.equals(entity.getId(), latest.getId())) {
                vo.setAlias(VersionAliasConverter.LATEST);
            }
            vo.setOwner(userService.findUserById(entity.getOwnerId()));
            return vo;
        });
    }

    public List<RuntimeVo> findRuntimeByVersionIds(List<Long> versionIds) {
        List<RuntimeVersionEntity> versions = runtimeVersionMapper.findByIds(
                Joiner.on(",").join(versionIds));

        return versions.stream().map(version -> {
            RuntimeEntity rt = runtimeMapper.find(version.getRuntimeId());
            RuntimeVo vo = runtimeConvertor.convert(rt);
            vo.setVersion(versionConvertor.convert(version));
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
        List<RuntimeVersionEntity> runtimeVersionEntities = runtimeVersionMapper
                .list(rt.getId(), null, null);

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
            //create
            entity = RuntimeEntity.builder().isDeleted(0)
                    .ownerId(userService.currentUserDetail().getId())
                    .projectId(projectId)
                    .runtimeName(uploadRequest.name())
                    .build();
            runtimeMapper.insert(entity);
        }
        log.debug("Runtime checked time use {}", System.currentTimeMillis() - startTime);
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.findByNameAndRuntimeId(uploadRequest.version(),
                entity.getId());
        boolean entityExists = (null != runtimeVersionEntity);
        if (entityExists && !uploadRequest.force()) {
            log.debug("Runtime version checked time use {}", System.currentTimeMillis() - startTime);
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.RUNTIME,
                            "Runtime version duplicate" + uploadRequest.version()),
                    HttpStatus.BAD_REQUEST);
        } else if (entityExists && uploadRequest.force()) {
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                    .parallelStream().forEach(job -> {
                        JobRuntime runtime = job.getJobRuntime();
                        if (runtime.getName().equals(uploadRequest.name()) && runtime.getVersion()
                                .equals(uploadRequest.version())) {
                            throw new StarwhaleApiException(
                                    new SwValidationException(ValidSubject.RUNTIME,
                                            "job's are running on runtime version " + uploadRequest.version()
                                                    + " you can't force push now"),
                                    HttpStatus.BAD_REQUEST);
                        }
                    });
        }
        log.debug("Runtime version checked time use {}", System.currentTimeMillis() - startTime);
        //upload to storage
        final String runtimePath = entityExists ? runtimeVersionEntity.getStoragePath()
                : storagePathCoordinator.allocateRuntimePath(project.getId(), uploadRequest.name(),
                        uploadRequest.version());

        try (final InputStream inputStream = dsFile.getInputStream()) {
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH, runtimePath, dsFile.getOriginalFilename()),
                    inputStream, dsFile.getSize());
        } catch (IOException e) {
            log.error("upload runtime failed {}", uploadRequest.getRuntime(), e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        /* create new entity */
        if (!entityExists) {
            RuntimeManifest runtimeManifestObj;
            String runtimeManifest;
            try (final InputStream inputStream = dsFile.getInputStream()) {
                // only extract the eval job file content
                runtimeManifest = new String(
                        Objects.requireNonNull(
                                TarFileUtil.getContentFromTarFile(inputStream, "", "_manifest.yaml")));
                runtimeManifestObj = yamlMapper.readValue(runtimeManifest,
                        RuntimeManifest.class);
            } catch (IOException e) {
                log.error("upload runtime failed {}", uploadRequest.getRuntime(), e);
                throw new StarwhaleApiException(new SwProcessException(ErrorType.SYSTEM),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            RuntimeVersionEntity version = RuntimeVersionEntity.builder()
                    .ownerId(userService.currentUserDetail().getId())
                    .storagePath(runtimePath)
                    .runtimeId(entity.getId())
                    .versionName(uploadRequest.version())
                    .versionMeta(runtimeManifest)
                    .image(null == runtimeManifestObj ? null : runtimeManifestObj.getBaseImage())
                    .build();
            runtimeVersionMapper.insert(version);
            RevertManager.create(bundleManager, runtimeDao)
                    .revertVersionTo(version.getRuntimeId(), version.getId());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RuntimeManifest {

        @JsonProperty("base_image")
        String baseImage;
    }

    public void pull(String projectUrl, String runtimeUrl, String versionUrl, HttpServletResponse httpResponse) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl));
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.find(versionId);
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
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl.create(projectUrl, runtimeUrl, versionUrl));
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.find(versionId);
        if (null == runtimeVersionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "Not found.");
        }
        return runtimeVersionEntity.getName();
    }

    public Boolean recoverRuntime(String projectUrl, String runtimeUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }
}
