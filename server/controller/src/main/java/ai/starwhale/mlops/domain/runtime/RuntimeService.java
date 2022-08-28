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

import ai.starwhale.mlops.api.protocol.StorageFileVO;
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVO;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVO;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleURL;
import ai.starwhale.mlops.domain.bundle.BundleVersionURL;
import ai.starwhale.mlops.domain.bundle.recover.RecoverException;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import ai.starwhale.mlops.domain.bundle.tag.TagManager;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
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

    @Resource
    private RuntimeMapper runtimeMapper;

    @Resource
    private RuntimeVersionMapper runtimeVersionMapper;

    @Resource
    private StorageService storageService;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private RuntimeConvertor runtimeConvertor;

    @Resource
    private RuntimeVersionConvertor versionConvertor;

    @Resource
    private RuntimeManager runtimeManager;

    @Resource
    private StoragePathCoordinator storagePathCoordinator;

    @Resource
    private StorageAccessService storageAccessService;

    @Resource
    private UserService userService;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private HotJobHolder jobHolder;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    private BundleManager bundleManager() {
        return new BundleManager(idConvertor, projectManager, runtimeManager, runtimeManager, ValidSubject.RUNTIME);
    }

    public PageInfo<RuntimeVO> listRuntime(RuntimeQuery runtimeQuery, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(runtimeQuery.getProjectUrl());
        List<RuntimeEntity> entities = runtimeMapper.listRuntimes(projectId, runtimeQuery.getNamePrefix());

        return PageUtil.toPageInfo(entities, rt -> {
            RuntimeVO vo = runtimeConvertor.convert(rt);
            RuntimeVersionEntity version = runtimeVersionMapper.getLatestVersion(rt.getId());
            if(version != null) {
                vo.setVersion(versionConvertor.convert(version));
            }
            return vo;
        });
    }

    public Boolean deleteRuntime(RuntimeQuery runtimeQuery) {
        return RemoveManager.create(bundleManager(), runtimeManager)
            .removeBundle(BundleURL.create(runtimeQuery.getProjectUrl(), runtimeQuery.getRuntimeUrl()));
    }

    public RuntimeInfoVO getRuntimeInfo(RuntimeQuery runtimeQuery) {
        BundleManager bundleManager = bundleManager();
        BundleURL bundleURL = BundleURL.create(runtimeQuery.getProjectUrl(), runtimeQuery.getRuntimeUrl());
        Long runtimeId = bundleManager.getBundleId(bundleURL);
        RuntimeEntity rt = runtimeMapper.findRuntimeById(runtimeId);
        if(rt == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME)
                .tip("Unable to find runtime " + runtimeQuery.getRuntimeUrl()), HttpStatus.BAD_REQUEST);
        }

        RuntimeVersionEntity versionEntity = null;
        if(!StrUtil.isEmpty(runtimeQuery.getRuntimeVersionUrl())) {
            Long versionId = bundleManager.getBundleVersionId(BundleVersionURL
                .create(bundleURL, runtimeQuery.getRuntimeVersionUrl()), runtimeId);
            versionEntity = runtimeVersionMapper.findVersionById(versionId);
        }
        if(versionEntity == null) {
            versionEntity = runtimeVersionMapper.getLatestVersion(rt.getId());
        }
        if(versionEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME)
                .tip("Unable to find the latest version of runtime " + runtimeQuery.getRuntimeUrl()), HttpStatus.BAD_REQUEST);
        }

        return toRuntimeInfoVO(rt, versionEntity);
    }

    private RuntimeInfoVO toRuntimeInfoVO(RuntimeEntity rt, RuntimeVersionEntity versionEntity) {
        try {
            String storagePath = versionEntity.getStoragePath();
            List<StorageFileVO> collect = storageService.listStorageFile(storagePath);

            return RuntimeInfoVO.builder()
                .id(idConvertor.convert(rt.getId()))
                .name(rt.getRuntimeName())
                .versionName(versionEntity.getVersionName())
                .versionTag(versionEntity.getVersionTag())
                .versionMeta(versionEntity.getVersionMeta())
                .createdTime(localDateTimeConvertor.convert(versionEntity.getCreatedTime()))
                .files(collect)
                .build();

        } catch (IOException e) {
            log.error("list runtime storage", e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE)
                .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean modifyRuntimeVersion(String projectUrl, String runtimeUrl, String runtimeVersionUrl, RuntimeVersion version) {
        Long versionId = bundleManager().getBundleVersionId(BundleVersionURL
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
            return TagManager.create(bundleManager(), runtimeManager)
                .updateTag(BundleVersionURL.create(projectUrl, runtimeUrl, versionUrl), tagAction);
        } catch (TagException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME).tip(e.getMessage()),
                HttpStatus.BAD_REQUEST);
        }

    }

    public Boolean revertVersionTo(String projectUrl, String runtimeUrl, String runtimeVersionUrl) {
        return RevertManager.create(bundleManager(), runtimeManager)
            .revertVersionTo(BundleVersionURL.create(projectUrl, runtimeUrl, runtimeVersionUrl));
    }

    public PageInfo<RuntimeVersionVO> listRuntimeVersionHistory(RuntimeVersionQuery query, PageParams pageParams) {
        Long runtimeId = bundleManager().getBundleId(BundleURL.create(query.getProjectUrl(), query.getRuntimeUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<RuntimeVersionEntity> entities = runtimeVersionMapper.listVersions(
            runtimeId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, versionConvertor::convert);
    }

    public List<RuntimeVO> findRuntimeByVersionIds(List<Long> versionIDs) {
        List<RuntimeVersionEntity> versions = runtimeVersionMapper.findVersionsByIds(
            versionIDs);

        return versions.stream().map(version -> {
            RuntimeEntity rt = runtimeMapper.findRuntimeById(version.getRuntimeId());
            RuntimeVO vo = runtimeConvertor.convert(rt);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<RuntimeInfoVO> listRuntimeInfo(String project, String name) {
        if(StringUtils.hasText(name)){
            Long projectId = projectManager.getProjectId(project);
            RuntimeEntity rt = runtimeMapper.findByName(name, projectId);
            if(rt == null) {
                throw new SWValidationException(ValidSubject.RUNTIME)
                    .tip("Unable to find the runtime with name " + name);
            }
            return runtimeInfoOfRuntime(rt);
        }

        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project, userService.currentUserDetail().getIdTableKey());
        List<RuntimeEntity> runtimeEntities = runtimeMapper.listRuntimes(projectEntity.getId(), null);
        if(runtimeEntities == null || runtimeEntities.isEmpty()) {
            return List.of();
        }

        return runtimeEntities.parallelStream()
            .map(this::runtimeInfoOfRuntime)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<RuntimeInfoVO> runtimeInfoOfRuntime(RuntimeEntity rt) {
        List<RuntimeVersionEntity> runtimeVersionEntities = runtimeVersionMapper
            .listVersions(rt.getId(), null, null);

        if(runtimeVersionEntities == null || runtimeVersionEntities.isEmpty()) {
            return List.of();
        }

        return runtimeVersionEntities.parallelStream()
            .map(entity -> toRuntimeInfoVO(rt, entity)).collect(Collectors.toList());
    }

    static final String FORMATTER_STORAGE_PATH = "%s/%s";
    @Transactional
    public void upload(MultipartFile dsFile, ClientRuntimeRequest uploadRequest){

        long startTime = System.currentTimeMillis();
        log.debug("access received at {}", startTime);
        ProjectEntity projectEntity = projectManager.getProject(uploadRequest.getProject());
        Long projectId = projectEntity.getId();
        RuntimeEntity entity = runtimeMapper.findByNameForUpdate(uploadRequest.name(), projectId);
        if (null == entity) {
            //create
            projectEntity = projectManager.findByNameOrDefault(uploadRequest.getProject(), userService.currentUserDetail().getIdTableKey());
            entity = RuntimeEntity.builder().isDeleted(0)
                .ownerId(userService.currentUserDetail().getId())
                .projectId(null == projectEntity ? null : projectEntity.getId())
                .runtimeName(uploadRequest.name())
                .build();
            runtimeMapper.addRuntime(entity);
        }
        log.debug("Runtime checked time use {}", System.currentTimeMillis() - startTime);
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.findByNameAndRuntimeId(uploadRequest.version(), entity.getId());
        boolean entityExists = (null != runtimeVersionEntity);
        if(entityExists && !uploadRequest.force()){
            log.debug("Runtime version checked time use {}",System.currentTimeMillis() - startTime);
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME).tip("Runtime version duplicate" + uploadRequest.version()),
                HttpStatus.BAD_REQUEST);
        } else if (entityExists && uploadRequest.force()) {
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                .parallelStream().forEach(job -> {
                    JobRuntime runtime = job.getJobRuntime();
                    if(runtime.getName().equals(uploadRequest.name()) && runtime.getVersion().equals(uploadRequest.version())) {
                        throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME).tip("job's are running on runtime version "+uploadRequest.version() +" you can't force push now"),
                            HttpStatus.BAD_REQUEST);
                    }
                });
        }
        log.debug("Runtime version checked time use {}",System.currentTimeMillis() - startTime);
        //upload to storage
        final String runtimePath = entityExists ? runtimeVersionEntity.getStoragePath()
            : storagePathCoordinator.generateRuntimePath(projectEntity.getProjectName(), uploadRequest.name(), uploadRequest.version());

        try(final InputStream inputStream = dsFile.getInputStream()){
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH, runtimePath, dsFile.getOriginalFilename()), inputStream, dsFile.getSize());
        } catch (IOException e) {
            log.error("upload runtime failed {}",uploadRequest.getRuntime(),e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        /* create new entity */
        if(!entityExists) {
            runtimeVersionMapper.addNewVersion(RuntimeVersionEntity.builder()
                .ownerId(userService.currentUserDetail().getId())
                .storagePath(runtimePath)
                .runtimeId(entity.getId())
                .versionName(uploadRequest.version())
                .versionMeta(uploadRequest.getRuntime())
                .manifest(uploadRequest.getManifest())
                .build());
        }
    }

    public void pull(String projectUrl, String runtimeUrl, String versionUrl, HttpServletResponse httpResponse) {
        Long projectId = projectManager.getProjectId(projectUrl);
        RuntimeEntity runtimeEntity = runtimeMapper.findByName(runtimeUrl, projectId);
        if(null == runtimeEntity){
            throw new SWValidationException(ValidSubject.RUNTIME).tip("Runtime not found");
        }
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.findByNameAndRuntimeId(versionUrl, runtimeEntity.getId());
        if(null == runtimeVersionEntity){
            throw new SWValidationException(ValidSubject.RUNTIME).tip("Runtime version not found");
        }
        List<String> files;
        try {
            files = storageAccessService.list(
                runtimeVersionEntity.getStoragePath()).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listing runtime version files from storage failed {}", runtimeVersionEntity.getStoragePath(), e);
            throw new SWProcessException(ErrorType.STORAGE);
        }
        if(CollectionUtils.isEmpty(files)){
            throw new SWValidationException(ValidSubject.RUNTIME).tip("Runtime version empty folder");
        }
        String filePath = files.get(0);
        try(InputStream fileInputStream = storageAccessService.get(filePath); ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            String fileName = filePath.substring(runtimeVersionEntity.getStoragePath().length() + 1);
            httpResponse.addHeader("Content-Disposition","attachment; filename=\""+fileName+"\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download runtime file from storage failed {}",runtimeVersionEntity.getStoragePath(),e);
            throw new SWProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String runtimeUrl, String versionUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        RuntimeEntity entity = runtimeMapper.findByName(runtimeUrl, projectId);
        if(null == entity){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME),HttpStatus.NOT_FOUND);
        }
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.findByNameAndRuntimeId(
            versionUrl, entity.getId());
        if(null == runtimeVersionEntity){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME),HttpStatus.NOT_FOUND);
        }
        return runtimeVersionEntity.getName();
    }

    public Boolean recoverRuntime(String projectUrl, String runtimeUrl) {
        try {
            return RecoverManager.create(projectManager, runtimeManager, idConvertor)
                    .recoverBundle(BundleURL.create(projectUrl, runtimeUrl));
        } catch (RecoverException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.RUNTIME).tip(e.getMessage()),HttpStatus.BAD_REQUEST);
        }
    }
}
