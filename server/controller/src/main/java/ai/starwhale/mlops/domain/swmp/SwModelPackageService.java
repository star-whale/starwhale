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

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.api.protocol.StorageFileVo;
import ai.starwhale.mlops.api.protocol.swmp.ClientSwmpRequest;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageInfoVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVersionVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import ai.starwhale.mlops.domain.bundle.tag.TagManager;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swmp.bo.SwmpQuery;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersion;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersionQuery;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.StarwhaleException;
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
public class SwModelPackageService {

    private final SwModelPackageMapper swmpMapper;
    private final SwModelPackageVersionMapper swmpVersionMapper;
    private final IdConvertor idConvertor;
    private final VersionAliasConvertor versionAliasConvertor;
    private final SwmpConvertor swmpConvertor;
    private final SwmpVersionConvertor versionConvertor;
    private final StoragePathCoordinator storagePathCoordinator;
    private final StorageAccessService storageAccessService;
    private final StorageService storageService;
    private final UserService userService;
    private final ProjectManager projectManager;
    private final SwmpManager swmpManager;
    private final HotJobHolder jobHolder;
    @Setter
    private BundleManager bundleManager;

    public SwModelPackageService(SwModelPackageMapper swmpMapper, SwModelPackageVersionMapper swmpVersionMapper,
            IdConvertor idConvertor, VersionAliasConvertor versionAliasConvertor, SwmpConvertor swmpConvertor,
            SwmpVersionConvertor versionConvertor, StoragePathCoordinator storagePathCoordinator,
            SwmpManager swmpManager, StorageAccessService storageAccessService, StorageService storageService,
            UserService userService, ProjectManager projectManager, HotJobHolder jobHolder) {
        this.swmpMapper = swmpMapper;
        this.swmpVersionMapper = swmpVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.swmpConvertor = swmpConvertor;
        this.versionConvertor = versionConvertor;
        this.storagePathCoordinator = storagePathCoordinator;
        this.swmpManager = swmpManager;
        this.storageAccessService = storageAccessService;
        this.storageService = storageService;
        this.userService = userService;
        this.projectManager = projectManager;
        this.jobHolder = jobHolder;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectManager,
                swmpManager,
                swmpManager
        );
    }

    public PageInfo<SwModelPackageVo> listSwmp(SwmpQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<SwModelPackageEntity> entities = swmpMapper.listSwModelPackages(projectId, query.getNamePrefix());
        return PageUtil.toPageInfo(entities, swmpConvertor::convert);
    }

    public Boolean deleteSwmp(SwmpQuery query) {
        return RemoveManager.create(bundleManager, swmpManager)
                .removeBundle(BundleUrl.create(query.getProjectUrl(), query.getSwmpUrl()));
    }

    public Boolean recoverSwmp(String projectUrl, String modelUrl) {
        try {
            return RecoverManager.create(projectManager, swmpManager, idConvertor)
                    .recoverBundle(BundleUrl.create(projectUrl, modelUrl));
        } catch (StarwhaleException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP).tip(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public List<SwModelPackageInfoVo> listSwmpInfo(String project, String name) {

        if (StringUtils.hasText(name)) {
            Long projectId = projectManager.getProjectId(project);
            SwModelPackageEntity swmp = swmpMapper.findByName(name, projectId);
            if (swmp == null) {
                throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP)
                        .tip("Unable to find the swmp with name " + name), HttpStatus.BAD_REQUEST);
            }
            return listSwmpInfoOfModel(swmp);
        }

        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project,
                userService.currentUserDetail().getIdTableKey());
        List<SwModelPackageEntity> entities = swmpMapper.listSwModelPackages(projectEntity.getId(), null);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.parallelStream()
                .map(this::listSwmpInfoOfModel)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<SwModelPackageInfoVo> listSwmpInfoOfModel(SwModelPackageEntity model) {
        List<SwModelPackageVersionEntity> versions = swmpVersionMapper.listVersions(
                model.getId(), null, null);
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        return versions.parallelStream()
                .map(version -> toSwModelPackageInfoVo(model, version))
                .collect(Collectors.toList());
    }

    public SwModelPackageInfoVo getSwmpInfo(SwmpQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getSwmpUrl());
        Long swmpId = bundleManager.getBundleId(bundleUrl);
        SwModelPackageEntity model = swmpMapper.findSwModelPackageById(swmpId);
        if (model == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP)
                    .tip("Unable to find swmp " + query.getSwmpUrl()), HttpStatus.BAD_REQUEST);
        }

        SwModelPackageVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(query.getSwmpVersionUrl())) {
            // find version by versionId
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(bundleUrl, query.getSwmpVersionUrl()), swmpId);
            versionEntity = swmpVersionMapper.findVersionById(versionId);
        }
        if (versionEntity == null) {
            // find current version
            versionEntity = swmpVersionMapper.getLatestVersion(model.getId());
        }
        if (versionEntity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP)
                    .tip("Unable to find the version of swmp " + query.getSwmpUrl()), HttpStatus.BAD_REQUEST);
        }

        return toSwModelPackageInfoVo(model, versionEntity);
    }

    private SwModelPackageInfoVo toSwModelPackageInfoVo(SwModelPackageEntity model,
            SwModelPackageVersionEntity version) {

        //Get file list in storage
        try {
            String storagePath = version.getStoragePath();
            List<StorageFileVo> collect = storageService.listStorageFile(storagePath);

            return SwModelPackageInfoVo.builder()
                    .id(idConvertor.convert(model.getId()))
                    .name(model.getSwmpName())
                    .versionAlias(versionAliasConvertor.convert(version.getVersionOrder()))
                    .versionName(version.getVersionName())
                    .versionTag(version.getVersionTag())
                    .versionMeta(version.getVersionMeta())
                    .manifest(version.getManifest())
                    .createdTime(version.getCreatedTime().getTime())
                    .files(collect)
                    .build();

        } catch (IOException e) {
            log.error("list swmp storage", e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE)
                    .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean modifySwmpVersion(String projectUrl, String swmpUrl, String versionUrl, SwmpVersion version) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                .create(projectUrl, swmpUrl, versionUrl));
        SwModelPackageVersionEntity entity = SwModelPackageVersionEntity.builder()
                .id(versionId)
                .versionTag(version.getTag())
                .build();
        int update = swmpVersionMapper.update(entity);
        log.info("SwmpVersion has been modified. ID={}", version.getId());
        return update > 0;
    }


    public Boolean manageVersionTag(String projectUrl, String modelUrl, String versionUrl,
            TagAction tagAction) {
        try {
            return TagManager.create(bundleManager, swmpManager)
                    .updateTag(
                            BundleVersionUrl.create(projectUrl, modelUrl, versionUrl),
                            tagAction);
        } catch (TagException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP).tip(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public Boolean revertVersionTo(String projectUrl, String swmpUrl, String versionUrl) {
        return RevertManager.create(bundleManager, swmpManager)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, swmpUrl, versionUrl));
    }

    public PageInfo<SwModelPackageVersionVo> listSwmpVersionHistory(SwmpVersionQuery query, PageParams pageParams) {
        Long swmpId = bundleManager.getBundleId(BundleUrl
                .create(query.getProjectUrl(), query.getSwmpUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SwModelPackageVersionEntity> entities = swmpVersionMapper.listVersions(
                swmpId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, entity -> {
            SwModelPackageVersionVo vo = versionConvertor.convert(entity);
            vo.setSize(storageService.getStorageSize(entity.getStoragePath()));
            return vo;
        });
    }

    public List<SwModelPackageVo> findModelByVersionId(List<Long> versionIds) {

        List<SwModelPackageVersionEntity> versions = swmpVersionMapper.findVersionsByIds(versionIds);

        List<Long> ids = versions.stream()
                .map(SwModelPackageVersionEntity::getSwmpId)
                .collect(Collectors.toList());

        List<SwModelPackageEntity> models = swmpMapper.findSwModelPackagesByIds(ids);

        return models.stream()
                .map(swmpConvertor::convert)
                .collect(Collectors.toList());
    }


    /**
     * prefix + / + fileName
     */
    static final String FORMATTER_STORAGE_PATH = "%s/%s";

    @Transactional
    public void upload(MultipartFile dsFile,
            ClientSwmpRequest uploadRequest) {

        long startTime = System.currentTimeMillis();
        log.debug("access received at {}", startTime);
        Long projectId = null;
        ProjectEntity projectEntity = null;
        if (!StrUtil.isEmpty(uploadRequest.getProject())) {
            projectEntity = projectManager.getProject(uploadRequest.getProject());
            projectId = projectEntity.getId();
        }
        SwModelPackageEntity entity = swmpMapper.findByNameForUpdate(uploadRequest.name(), projectId);
        if (null == entity) {
            //create
            if (projectId == null) {
                projectEntity = projectManager.findByNameOrDefault(uploadRequest.getProject(),
                        userService.currentUserDetail().getIdTableKey());
                projectId = projectEntity.getId();
            }
            entity = SwModelPackageEntity.builder().isDeleted(0)
                    .ownerId(getOwner())
                    .projectId(projectId)
                    .swmpName(uploadRequest.name())
                    .build();
            swmpMapper.addSwModelPackage(entity);
        }
        log.debug("swmp checked time use {}", System.currentTimeMillis() - startTime);
        SwModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(
                uploadRequest.version(), entity.getId());
        boolean entityExists = null != swModelPackageVersionEntity;
        if (entityExists && !uploadRequest.force()) {
            log.debug("swmp version checked time use {}", System.currentTimeMillis() - startTime);
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP).tip(
                    "swmp version duplicate" + uploadRequest.version()),
                    HttpStatus.BAD_REQUEST);
        } else if (entityExists && uploadRequest.force()) {
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                    .parallelStream().forEach(job -> {
                        SwModelPackage swmp = job.getSwmp();
                        if (swmp.getName().equals(uploadRequest.name())
                                && swmp.getVersion().equals(uploadRequest.version())) {
                            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP).tip(
                                    "job's are running on swmp version " + uploadRequest.version()
                                            + " you can't force push now"),
                                    HttpStatus.BAD_REQUEST);
                        }
                    });
        }
        log.debug("swmp version checked time use {}", System.currentTimeMillis() - startTime);
        //upload to storage
        final String swmpPath = entityExists ? swModelPackageVersionEntity.getStoragePath()
                : storagePathCoordinator.generateSwmpPath(projectEntity.getProjectName(), uploadRequest.name(),
                        uploadRequest.version());
        String jobContent = "";
        try (final InputStream inputStream = dsFile.getInputStream()) {
            // only extract the eval job file content
            jobContent = new String(
                    Objects.requireNonNull(
                            TarFileUtil.getContentFromTarFile(dsFile.getInputStream(), "src", "eval_jobs.yaml")));
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH, swmpPath, dsFile.getOriginalFilename()),
                    inputStream, dsFile.getSize());
        } catch (IOException e) {
            log.error("upload swmp failed {}", uploadRequest.getSwmp(), e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (entityExists) {
            // update job content
            swModelPackageVersionEntity.setEvalJobs(jobContent);
            swmpVersionMapper.update(swModelPackageVersionEntity);
        } else {
            // create new entity
            swModelPackageVersionEntity = SwModelPackageVersionEntity.builder()
                    .ownerId(getOwner())
                    .storagePath(swmpPath)
                    .swmpId(entity.getId())
                    .versionName(uploadRequest.version())
                    .versionMeta(uploadRequest.getSwmp())
                    .manifest(uploadRequest.getManifest())
                    .evalJobs(jobContent)
                    .build();
            swmpVersionMapper.addNewVersion(swModelPackageVersionEntity);
            swmpVersionMapper.revertTo(swModelPackageVersionEntity.getSwmpId(), swModelPackageVersionEntity.getId());
        }

    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if (null == currentUserDetail) {
            throw new SwAuthException(SwAuthException.AuthType.SWMP_UPLOAD);
        }
        return currentUserDetail.getIdTableKey();
    }

    public void pull(String projectUrl, String modelUrl, String versionUrl, HttpServletResponse httpResponse) {
        Long projectId = projectManager.getProjectId(projectUrl);
        SwModelPackageEntity swModelPackageEntity = swmpMapper.findByName(modelUrl, projectId);
        if (null == swModelPackageEntity) {
            throw new SwValidationException(ValidSubject.SWMP).tip("swmp not found");
        }
        SwModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(
                versionUrl, swModelPackageEntity.getId());
        if (null == swModelPackageVersionEntity) {
            throw new SwValidationException(ValidSubject.SWMP).tip("swmp version not found");
        }
        List<String> files;
        try {
            files = storageAccessService.list(
                    swModelPackageVersionEntity.getStoragePath()).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listing file from storage failed {}", swModelPackageVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }
        if (CollectionUtils.isEmpty(files)) {
            throw new SwValidationException(ValidSubject.SWMP).tip("swmp version empty folder");
        }
        String filePath = files.get(0);
        try (InputStream fileInputStream = storageAccessService.get(filePath);
                ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            String fileName = filePath.substring(swModelPackageVersionEntity.getStoragePath().length() + 1);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download file from storage failed {}", swModelPackageVersionEntity.getStoragePath(), e);
            throw new SwProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String modelUrl, String versionUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        SwModelPackageEntity entity = swmpMapper.findByName(modelUrl, projectId);
        if (null == entity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP), HttpStatus.NOT_FOUND);
        }
        SwModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(versionUrl,
                entity.getId());
        if (null == swModelPackageVersionEntity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP), HttpStatus.NOT_FOUND);
        }
        return "";
    }
}
