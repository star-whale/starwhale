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

import ai.starwhale.mlops.api.protocol.StorageFileVO;
import ai.starwhale.mlops.api.protocol.swmp.ClientSWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.TarFileUtil;
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
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swmp.bo.SWMPQuery;
import ai.starwhale.mlops.domain.swmp.bo.SWMPVersion;
import ai.starwhale.mlops.domain.swmp.bo.SWMPVersionQuery;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageVersionEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWAuthException;
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
import java.util.Objects;
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
public class SWModelPackageService {

    @Resource
    private SWModelPackageMapper swmpMapper;

    @Resource
    private SWModelPackageVersionMapper swmpVersionMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Resource
    private SWMPConvertor swmpConvertor;

    @Resource
    private SWMPVersionConvertor versionConvertor;

    @Resource
    private StoragePathCoordinator storagePathCoordinator;

    @Resource
    private StorageAccessService storageAccessService;

    @Resource
    private StorageService storageService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private HotJobHolder jobHolder;

    @Resource
    private SwmpManager swmpManager;

    public PageInfo<SWModelPackageVO> listSWMP(SWMPQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<SWModelPackageEntity> entities = swmpMapper.listSWModelPackages(projectId, query.getNamePrefix());
        return PageUtil.toPageInfo(entities, swmpConvertor::convert);
    }

    public Boolean deleteSWMP(SWMPQuery query) {
        return RemoveManager.create(bundleManager(), swmpManager)
            .removeBundle(BundleURL.create(query.getProjectUrl(), query.getSwmpUrl()));
    }

    public Boolean recoverSWMP(String projectUrl, String modelUrl) {
        try {
            return RecoverManager.create(projectManager, swmpManager, idConvertor)
                .recoverBundle(BundleURL.create(projectUrl, modelUrl));
        } catch (RecoverException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP).tip(e.getMessage()),HttpStatus.BAD_REQUEST);
        }
    }

    public List<SWModelPackageInfoVO> listSWMPInfo(String project, String name) {

        if(StringUtils.hasText(name)){
            Long projectId = projectManager.getProjectId(project);
            SWModelPackageEntity swmp = swmpMapper.findByName(name, projectId);
            if(swmp == null) {
                throw new SWValidationException(ValidSubject.SWMP)
                    .tip("Unable to find the swmp with name " + name);
            }
            return listSWMPInfoOfModel(swmp);
        }

        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project, userService.currentUserDetail().getIdTableKey());
        List<SWModelPackageEntity> entities = swmpMapper.listSWModelPackages(projectEntity.getId(), null);
        if(entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.parallelStream()
            .map(this::listSWMPInfoOfModel)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public List<SWModelPackageInfoVO> listSWMPInfoOfModel(SWModelPackageEntity model) {
        List<SWModelPackageVersionEntity> versions = swmpVersionMapper.listVersions(
            model.getId(), null, null);
        if(versions == null || versions.isEmpty()) {
            return List.of();
        }
        return versions.parallelStream()
            .map(version -> toSWModelPackageInfoVO(model, version))
            .collect(Collectors.toList());
    }

    public SWModelPackageInfoVO getSWMPInfo(SWMPQuery query) {
        BundleManager bundleManager = bundleManager();
        BundleURL bundleURL = BundleURL.create(query.getProjectUrl(), query.getSwmpUrl());
        Long swmpId = bundleManager.getBundleId(bundleURL);
        SWModelPackageEntity model = swmpMapper.findSWModelPackageById(swmpId);
        if (model == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip("Unable to find swmp " + query.getSwmpUrl()), HttpStatus.BAD_REQUEST);
        }

        SWModelPackageVersionEntity versionEntity = null;
        if(!StrUtil.isEmpty(query.getSwmpVersionUrl())) {
            // find version by versionId
            Long versionId = bundleManager.getBundleVersionId(BundleVersionURL
                .create(bundleURL, query.getSwmpVersionUrl()));
            versionEntity = swmpVersionMapper.findVersionById(versionId);
        }
        if(versionEntity == null) {
            // find current version
            versionEntity = swmpVersionMapper.getLatestVersion(model.getId());
        }
        if(versionEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip("Unable to find the version of swmp " + query.getSwmpUrl()), HttpStatus.BAD_REQUEST);
        }

        return toSWModelPackageInfoVO(model, versionEntity);
    }

    private SWModelPackageInfoVO toSWModelPackageInfoVO(SWModelPackageEntity model,
        SWModelPackageVersionEntity version) {

        //Get file list in storage
        try {
            String storagePath = version.getStoragePath();
            List<StorageFileVO> collect = storageService.listStorageFile(storagePath);

            return SWModelPackageInfoVO.builder()
                .id(idConvertor.convert(model.getId()))
                .name(model.getSwmpName())
                .versionName(version.getVersionName())
                .versionTag(version.getVersionTag())
                .versionMeta(version.getVersionMeta())
                .manifest(version.getManifest())
                .createdTime(localDateTimeConvertor.convert(version.getCreatedTime()))
                .files(collect)
                .build();

        } catch (IOException e) {
            log.error("list swmp storage", e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE)
                .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean modifySWMPVersion(String projectUrl, String swmpUrl, String versionUrl, SWMPVersion version) {
        Long versionId = bundleManager().getBundleVersionId(BundleVersionURL
            .create(projectUrl, swmpUrl, versionUrl));
        SWModelPackageVersionEntity entity = SWModelPackageVersionEntity.builder()
            .id(versionId)
            .versionTag(version.getTag())
            .build();
        int update = swmpVersionMapper.update(entity);
        log.info("SWMPVersion has been modified. ID={}", version.getId());
        return update > 0;
    }


    public Boolean manageVersionTag(String projectUrl, String modelUrl, String versionUrl,
        TagAction tagAction) {
        try {
            return TagManager.create(bundleManager(), swmpManager)
                .updateTag(
                    BundleVersionURL.create(projectUrl, modelUrl, versionUrl),
                    tagAction);
        } catch (TagException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP).tip(e.getMessage()),
                HttpStatus.BAD_REQUEST);
        }
    }

    public Boolean revertVersionTo(String projectUrl, String swmpUrl, String versionUrl) {
        return RevertManager.create(bundleManager(), swmpManager)
            .revertVersionTo(BundleVersionURL.create(projectUrl, swmpUrl, versionUrl));
    }

    public PageInfo<SWModelPackageVersionVO> listSWMPVersionHistory(SWMPVersionQuery query, PageParams pageParams) {
        Long swmpId = bundleManager().getBundleId(BundleURL
            .create(query.getProjectUrl(), query.getSwmpUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWModelPackageVersionEntity> entities = swmpVersionMapper.listVersions(
            swmpId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, entity -> {
            SWModelPackageVersionVO vo = versionConvertor.convert(entity);
            vo.setSize(storageService.getStorageSize(entity.getStoragePath()));
            return vo;
        });
    }

    public List<SWModelPackageVO> findModelByVersionId(List<Long> versionIds) {

        List<SWModelPackageVersionEntity> versions = swmpVersionMapper.findVersionsByIds(versionIds);

        List<Long> ids = versions.stream()
            .map(SWModelPackageVersionEntity::getSwmpId)
            .collect(Collectors.toList());

        List<SWModelPackageEntity> models = swmpMapper.findSWModelPackagesByIds(ids);

        return models.stream()
            .map(swmpConvertor::convert)
            .collect(Collectors.toList());
    }


    /**
     * prefix + / + fileName
     */
    static final String FORMATTER_STORAGE_PATH="%s/%s";
    @Transactional
    public void upload(MultipartFile dsFile,
        ClientSWMPRequest uploadRequest){

        long startTime = System.currentTimeMillis();
        log.debug("access received at {}",startTime);
        Long projectId = null;
        ProjectEntity projectEntity = null;
        if(!StrUtil.isEmpty(uploadRequest.getProject())) {
            projectEntity = projectManager.getProject(uploadRequest.getProject());
            projectId = projectEntity.getId();
        }
        SWModelPackageEntity entity = swmpMapper.findByNameForUpdate(uploadRequest.name(), projectId);
        if (null == entity) {
            //create
            if(projectId == null) {
                projectEntity = projectManager.findByNameOrDefault(uploadRequest.getProject(), userService.currentUserDetail().getIdTableKey());
                projectId = projectEntity.getId();
            }
            entity = SWModelPackageEntity.builder().isDeleted(0)
                .ownerId(getOwner())
                .projectId(projectId)
                .swmpName(uploadRequest.name())
                .build();
            swmpMapper.addSWModelPackage(entity);
        }
        log.debug("swmp checked time use {}", System.currentTimeMillis() - startTime);
        SWModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(uploadRequest.version(),entity.getId());
        boolean entityExists = null != swModelPackageVersionEntity;
        if(entityExists && !uploadRequest.force()){
            log.debug("swmp version checked time use {}",System.currentTimeMillis() - startTime);
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP).tip("swmp version duplicate"+uploadRequest.version()),
                HttpStatus.BAD_REQUEST);
        }else if(entityExists && uploadRequest.force()){
            jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                .parallelStream().forEach(job -> {
                    SWModelPackage swmp = job.getSwmp();
                    if(swmp.getName().equals(uploadRequest.name()) && swmp.getVersion().equals(uploadRequest.version())){
                        throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP).tip("job's are running on swmp version "+uploadRequest.version() +" you can't force push now"),
                            HttpStatus.BAD_REQUEST);
                    }
                });
        }
        log.debug("swmp version checked time use {}",System.currentTimeMillis() - startTime);
        //upload to storage
        final String swmpPath = entityExists ? swModelPackageVersionEntity.getStoragePath()
            : storagePathCoordinator.generateSwmpPath(projectEntity.getProjectName(),uploadRequest.name(), uploadRequest.version());
        String jobContent = "";
        try(final InputStream inputStream = dsFile.getInputStream()){
            // only extract the eval job file content
            jobContent = new String(
                Objects.requireNonNull(TarFileUtil.getContentFromTarFile(dsFile.getInputStream(), "src", "eval_jobs.yaml")));
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH,swmpPath,dsFile.getOriginalFilename()),inputStream, dsFile.getSize());
        } catch (IOException e) {
            log.error("upload swmp failed {}",uploadRequest.getSwmp(),e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(entityExists){
            // update job content
            swModelPackageVersionEntity.setEvalJobs(jobContent);
            swmpVersionMapper.update(swModelPackageVersionEntity);
        } else {
            // create new entity
            swModelPackageVersionEntity = SWModelPackageVersionEntity.builder()
                .ownerId(getOwner())
                .storagePath(swmpPath)
                .swmpId(entity.getId())
                .versionName(uploadRequest.version())
                .versionMeta(uploadRequest.getSwmp())
                .manifest(uploadRequest.getManifest())
                .evalJobs(jobContent)
                .build();
            swmpVersionMapper.addNewVersion(swModelPackageVersionEntity);
        }

    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if(null == currentUserDetail){
            throw new SWAuthException(SWAuthException.AuthType.SWMP_UPLOAD);
        }
        return currentUserDetail.getIdTableKey();
    }

    public void pull(String projectUrl, String modelUrl, String versionUrl, HttpServletResponse httpResponse) {
        Long projectId = projectManager.getProjectId(projectUrl);
        SWModelPackageEntity swModelPackageEntity = swmpMapper.findByName(modelUrl, projectId);
        if(null == swModelPackageEntity){
            throw new SWValidationException(ValidSubject.SWMP).tip("swmp not found");
        }
        SWModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(
            versionUrl, swModelPackageEntity.getId());
        if(null == swModelPackageVersionEntity){
            throw new SWValidationException(ValidSubject.SWMP).tip("swmp version not found");
        }
        List<String> files;
        try {
            files = storageAccessService.list(
                swModelPackageVersionEntity.getStoragePath()).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("listing file from storage failed {}",swModelPackageVersionEntity.getStoragePath(),e);
            throw new SWProcessException(ErrorType.STORAGE);
        }
        if(CollectionUtils.isEmpty(files)){
            throw new SWValidationException(ValidSubject.SWMP).tip("swmp version empty folder");
        }
        String filePath = files.get(0);
        try(InputStream fileInputStream = storageAccessService.get(filePath);
            ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            String fileName = filePath.substring(swModelPackageVersionEntity.getStoragePath().length() + 1);
            httpResponse.addHeader("Content-Disposition","attachment; filename=\""+fileName+"\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download file from storage failed {}",swModelPackageVersionEntity.getStoragePath(),e);
            throw new SWProcessException(ErrorType.STORAGE);
        }

    }

    public String query(String projectUrl, String modelUrl, String versionUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        SWModelPackageEntity entity = swmpMapper.findByName(modelUrl, projectId);
        if(null == entity){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP),HttpStatus.NOT_FOUND);
        }
        SWModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(versionUrl, entity.getId());
        if(null == swModelPackageVersionEntity){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP),HttpStatus.NOT_FOUND);
        }
        return "";
    }

    private BundleManager bundleManager() {
        return new BundleManager(idConvertor, projectManager, swmpManager, swmpManager, ValidSubject.SWMP);
    }
}
