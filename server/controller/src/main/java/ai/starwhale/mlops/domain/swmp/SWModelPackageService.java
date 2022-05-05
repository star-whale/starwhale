/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.api.protocol.swmp.ClientSWMPRequest;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO.ModelFile;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swmp.SWMPObject.Version;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageVersionMapper;
import ai.starwhale.mlops.domain.task.LivingTaskCache;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SWAuthException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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
    private SWMPConvertor swmpConvertor;

    @Resource
    private SWMPVersionConvertor versionConvertor;

    @Resource
    private StoragePathCoordinator storagePathCoordinator;

    @Resource
    private StorageAccessService storageAccessService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private LivingTaskCache livingTaskCache;


    public PageInfo<SWModelPackageVO> listSWMP(SWMPObject swmp, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWModelPackageEntity> entities = swmpMapper.listSWModelPackages(
            idConvertor.revert(swmp.getProjectId()), swmp.getName());
        return PageUtil.toPageInfo(entities, swmpConvertor::convert);
    }

    public Boolean deleteSWMP(SWMPObject swmp) {
        Long id = idConvertor.revert(swmp.getId());
        int res = swmpMapper.deleteSWModelPackage(id);
        log.info("SWMP has been deleted. ID={}", id);
        return res > 0;
    }

    public SWModelPackageInfoVO getSWMPInfo(SWMPObject swmp) {
        Long modelID = idConvertor.revert(swmp.getId());
        SWModelPackageEntity model = swmpMapper.findSWModelPackageById(modelID);
        if (model == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip("Unable to find swmp " + modelID), HttpStatus.BAD_REQUEST);
        }

        SWModelPackageVersionEntity versionEntity;
        if(swmp.getVersion() != null) {
            // find version by versionId
            Long versionId = idConvertor.revert(swmp.getVersion().getId());
            versionEntity = swmpVersionMapper.findVersionById(versionId);
            if(versionEntity == null) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                    .tip("Unable to find the version of id " + versionId), HttpStatus.BAD_REQUEST);
            }
        } else {
            // find current version
            versionEntity = swmpVersionMapper.getLatestVersion(modelID);
            if(versionEntity == null) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                    .tip("Unable to find the latest version of swmp " + modelID), HttpStatus.BAD_REQUEST);
            }
        }

        //Get file list in storage
        try {
            String storagePath = versionEntity.getStoragePath();
            Stream<String> list = storageAccessService.list(storagePath);
            List<ModelFile> collect = list.map(filePath -> {
                long length = 0L;
                try {
                    StorageObjectInfo info = storageAccessService.head(filePath);
                    length = info.getContentLength();
                } catch (IOException e) {
                    log.error("storage head", e);
                }
                if (StrUtil.startWith(filePath, storagePath)) {
                    filePath = filePath.substring(0, storagePath.length());
                }
                return ModelFile.builder()
                    .name(filePath)
                    .size(FileUtil.readableFileSize(length))
                    .build();
            }).collect(Collectors.toList());

            return SWModelPackageInfoVO.builder()
                .modelName(model.getSwmpName())
                .files(collect)
                .build();

        } catch (IOException e) {
            log.error("list swmp storage", e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE)
                .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }

    public Boolean modifySWMPVersion(Version version) {
        SWModelPackageVersionEntity entity = SWModelPackageVersionEntity.builder()
            .id(idConvertor.revert(version.getId()))
            .versionTag(version.getTag())
            .storagePath(version.getStoragePath())
            .build();
        int update = swmpVersionMapper.update(entity);
        log.info("SWMPVersion has been modified. ID={}", version.getId());
        return update > 0;
    }

    public Boolean revertVersionTo(SWMPObject swmp) {
        Long vid = idConvertor.revert(swmp.getVersion().getId());
        int res = swmpVersionMapper.revertTo(vid);
        log.info("SWMP Version has been revert to {}", vid);
        return res > 0;
    }

    public PageInfo<SWModelPackageVersionVO> listSWMPVersionHistory(SWMPObject swmp, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWModelPackageVersionEntity> entities = swmpVersionMapper.listVersions(
            idConvertor.revert(swmp.getId()), swmp.getVersion().getName());
        return PageUtil.toPageInfo(entities, versionConvertor::convert);
    }

    public String addSWMP(SWMPObject swmp) {
        SWModelPackageEntity entity = SWModelPackageEntity.builder()
            .swmpName(swmp.getName())
            .ownerId(idConvertor.revert(swmp.getOwnerId()))
            .projectId(idConvertor.revert(swmp.getProjectId()))
            .build();
        if(entity.getProjectId() == 0) {
            ProjectEntity defaultProject = projectManager.findDefaultProject();
            if(defaultProject != null) {
                entity.setProjectId(defaultProject.getId());
            }
        }
        swmpMapper.addSWModelPackage(entity);
        log.info("SWMP has been created. ID={}, NAME={}", entity.getId(), entity.getSwmpName());
        return idConvertor.convert(entity.getId());
    }

    public String addVersion(SWMPObject swmp) {
        SWModelPackageVersionEntity entity = SWModelPackageVersionEntity.builder()
            .swmpId(idConvertor.revert(swmp.getId()))
            .ownerId(idConvertor.revert(swmp.getVersion().getOwnerId()))
            .versionTag(swmp.getVersion().getTag())
            .versionName(swmp.getVersion().getName())
            .versionMeta(swmp.getVersion().getMeta())
            .storagePath(swmp.getVersion().getStoragePath())
            .build();
        swmpVersionMapper.addNewVersion(entity);
        log.info("SWMP Version has been created. ID={}", entity.getId());
        return idConvertor.convert(entity.getId());
    }

    public List<SWModelPackageVO> findModelByVersionId(List<String> versionIds) {
        List<Long> vIds = versionIds.stream()
            .map(idConvertor::revert)
            .collect(Collectors.toList());

        List<SWModelPackageVersionEntity> versions = swmpVersionMapper.findVersionsByIds(vIds);

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

        Long startTime = System.currentTimeMillis();
        log.debug("access received at {}",startTime);
        SWModelPackageEntity entity = swmpMapper.findByNameForUpdate(uploadRequest.name());
        if (null == entity) {
            //create
            ProjectEntity projectEntity = projectManager.findByName(uploadRequest.getProject());
            entity = SWModelPackageEntity.builder().isDeleted(0)
                .ownerId(getOwner())
                .projectId(null == projectEntity ? null : projectEntity.getId())
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
            livingTaskCache.ofStatus(TaskStatus.RUNNING).parallelStream()
                .map(Task::getJob).collect(Collectors.toSet())
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
            : storagePathCoordinator.generateSwmpPath(uploadRequest.name(), uploadRequest.version());

        try(final InputStream inputStream = dsFile.getInputStream()){
            storageAccessService.put(String.format(FORMATTER_STORAGE_PATH,swmpPath,dsFile.getOriginalFilename()),inputStream);
        } catch (IOException e) {
            log.error("upload swmp failed {}",uploadRequest.getSwmp(),e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //create new entity
        if(!entityExists){
            swModelPackageVersionEntity = SWModelPackageVersionEntity.builder()
                .ownerId(getOwner())
                .storagePath(swmpPath)
                .swmpId(entity.getId())
                .versionName(uploadRequest.version())
                .versionMeta(uploadRequest.getSwmp())
                .build();
            swmpVersionMapper.addNewVersion(swModelPackageVersionEntity);
        }

    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if(null == currentUserDetail){
            throw new SWAuthException(SWAuthException.AuthType.SWMP_UPLOAD);
        }
        return Long.valueOf(currentUserDetail.getIdTableKey());
    }

    public byte[] pull(ClientSWMPRequest pullRequest) {
        SWModelPackageEntity swModelPackageEntity = swmpMapper.findByName(pullRequest.name());
        if(null == swModelPackageEntity){
            throw new SWValidationException(ValidSubject.SWMP).tip("swmp not found");
        }
        SWModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(
            pullRequest.version(), swModelPackageEntity.getId());
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
        try(InputStream fileInputStream = storageAccessService.get(
            files.get(0))) {
            return fileInputStream.readAllBytes();
        } catch (IOException e) {
            log.error("download file from storage failed {}",swModelPackageVersionEntity.getStoragePath(),e);
            throw new SWProcessException(ErrorType.STORAGE);
        }

    }

    public String query(ClientSWMPRequest queryRequest) {
        SWModelPackageEntity entity = swmpMapper.findByNameForUpdate(queryRequest.name());
        if(null == entity){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP),HttpStatus.NOT_FOUND);
        }
        SWModelPackageVersionEntity swModelPackageVersionEntity = swmpVersionMapper.findByNameAndSwmpId(queryRequest.version(),entity.getId());
        if(null == swModelPackageVersionEntity){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP),HttpStatus.NOT_FOUND);
        }
        return "";
    }
}
