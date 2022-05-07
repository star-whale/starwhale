/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.upload;

import static ai.starwhale.mlops.domain.swds.upload.SWDSVersionWithMetaConverter.EMPTY_YAML;

import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.common.util.Blake2bUtil;
import ai.starwhale.mlops.domain.project.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.SWDatasetEntity;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.domain.task.LivingTaskCache;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class SwdsUploader {

    final HotSwdsHolder hotSwdsHolder;

    final SWDatasetMapper swdsMapper;

    final SWDatasetVersionMapper swdsVersionMapper;

    final StoragePathCoordinator storagePathCoordinator;

    final StorageAccessService storageAccessService;

    final UserService userService;

    final ProjectMapper projectMapper;

    /**
     * prefix + / + fileName
     */
    static final String FORMATTER_STORAGE_PATH="%s/%s";

    final ObjectMapper yamlMapper;

    final LivingTaskCache livingTaskCache;

    final ProjectManager projectManager;

    public SwdsUploader(HotSwdsHolder hotSwdsHolder, SWDatasetMapper swdsMapper,
        SWDatasetVersionMapper swdsVersionMapper, StoragePathCoordinator storagePathCoordinator,
        StorageAccessService storageAccessService, UserService userService,
        ProjectMapper projectMapper,
        @Qualifier("yamlMapper") ObjectMapper yamlMapper,
        LivingTaskCache livingTaskCache,
        ProjectManager projectManager) {
        this.hotSwdsHolder = hotSwdsHolder;
        this.swdsMapper = swdsMapper;
        this.swdsVersionMapper = swdsVersionMapper;
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.userService = userService;
        this.projectMapper = projectMapper;
        this.yamlMapper = yamlMapper;
        this.livingTaskCache = livingTaskCache;
        this.projectManager = projectManager;
    }

    public void cancel(String uploadId){
        final SWDSVersionWithMeta swDatasetVersionEntityWithMeta = getSwdsVersion(uploadId);
        swdsVersionMapper.deleteById(swDatasetVersionEntityWithMeta.getSwDatasetVersionEntity().getId());
        hotSwdsHolder.cancel(uploadId);
        clearSwdsStorageData(swDatasetVersionEntityWithMeta.getSwDatasetVersionEntity());

    }

    private void clearSwdsStorageData(SWDatasetVersionEntity swDatasetVersionEntity) {
        final String storagePath = swDatasetVersionEntity.getStoragePath();
        try {
            Stream<String> files = storageAccessService.list(storagePath);
            files.parallel().forEach(file-> {
                try {
                    storageAccessService.delete(file);
                } catch (IOException e) {
                    log.error("clear file failed {}",file,e);
                }
            });
        } catch (IOException e) {
            log.error("delete storage objects failed for {}", swDatasetVersionEntity.getVersionName(),e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void uploadBody(String uploadId, MultipartFile file){
        final SWDSVersionWithMeta swDatasetVersionWithMeta = getSwdsVersion(uploadId);
        String filename = file.getOriginalFilename();
        byte[] fileBytes;
        try (InputStream inputStream = file.getInputStream()){
            fileBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("read swds failed {}", filename,e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.NETWORK),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        final SWDatasetVersionEntity swDatasetVersionEntity = swDatasetVersionWithMeta.getSwDatasetVersionEntity();
        synchronized (swDatasetVersionEntity){
            String digest = Blake2bUtil.digest(fileBytes);
            digestCheckWithManifest(swDatasetVersionWithMeta, filename, digest);
            if(fileUploaded(swDatasetVersionWithMeta, filename, digest)){
                log.info("file for {} {} already uploaded",uploadId,filename);
                return;
            }
            Map<String, String> uploadedFileBlake2bs = swDatasetVersionWithMeta.getVersionMeta()
                .getUploadedFileBlake2bs();
            uploadedFileBlake2bs.put(filename,digest);
            try {
                swDatasetVersionEntity.setFilesUploaded(yamlMapper.writeValueAsString(uploadedFileBlake2bs));
                swdsVersionMapper.updateFilesUploaded(swDatasetVersionEntity);
            } catch (JsonProcessingException e) {
                log.error("wirte map to string failed",e);
                throw new SWProcessException(ErrorType.DB).tip("write map to string failed");
            }
            uploadSwdsFileToStorage(filename, fileBytes, swDatasetVersionEntity);
        }

    }

    private void uploadSwdsFileToStorage(String filename, byte[] fileBytes,
        SWDatasetVersionEntity swDatasetVersionEntity) {
        final String storagePath = String.format(FORMATTER_STORAGE_PATH, swDatasetVersionEntity.getStoragePath(),
            filename);
        try{
            storageAccessService.put(storagePath,fileBytes);
        } catch (IOException e) {
            log.error("upload swds to failed {}", filename,e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    static final Pattern PATTERN_SIGNATURE=Pattern.compile("\\d+:blake2b:(.*)");
    private boolean fileUploaded(SWDSVersionWithMeta swdsVersionWithMeta, String filename,
        String digest) {
        Map<String, String> uploadedFileBlake2bs = swdsVersionWithMeta.getVersionMeta()
            .getUploadedFileBlake2bs();
        return digest.equals(uploadedFileBlake2bs.get(filename));
    }

    private void digestCheckWithManifest(SWDSVersionWithMeta swdsVersionWithMeta, String filename,
        String digest) {
        VersionMeta versionMeta = swdsVersionWithMeta.getVersionMeta();
        Map<String, String> signatures = versionMeta.getManifest().getSignature();
        String fileSignatureRaw = signatures.get(filename);
        if(null == fileSignatureRaw){
            log.warn("unexpected file uploaded {}",filename);
            return;
        }
        Matcher matcher = PATTERN_SIGNATURE.matcher(fileSignatureRaw);
        if(matcher.matches()){
            String fileSig = matcher.group(1);
            if(!fileSig.equals(digest)){
                log.error("signature matching failed for file {} expected {} actual {}",filename,fileSig,digest);
                throw new SWValidationException(ValidSubject.SWDS).tip("signature validation with file failed: " + filename);
            }
        }else {
            throw new SWValidationException(ValidSubject.SWDS).tip("signature pattern validation failed \\d+:blake2b:(.*)");
        }

    }

    SWDSVersionWithMeta getSwdsVersion(String uploadId) {
        final Optional<SWDSVersionWithMeta> swDatasetVersionEntityOpt = hotSwdsHolder.of(uploadId);
        return swDatasetVersionEntityOpt
            .orElseThrow(
                () -> new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("uploadId invalid"),
                    HttpStatus.BAD_REQUEST));
    }

    void uploadManifest(SWDatasetVersionEntity swDatasetVersionEntity,String fileName, byte[] bytes){
        uploadSwdsFileToStorage(fileName, bytes, swDatasetVersionEntity);
    }

    void reUploadManifest(SWDatasetVersionEntity swDatasetVersionEntity,String fileName, byte[] bytes){
        final String storagePath = String.format(FORMATTER_STORAGE_PATH,swDatasetVersionEntity.getStoragePath(),fileName);
        try{
            storageAccessService.delete(storagePath);
        } catch (IOException e) {
            log.warn("swds delete to failed {}",fileName,e);
        }
        uploadManifest(swDatasetVersionEntity,fileName,bytes);
    }



    @Transactional
    public String create(String yamlContent,String fileName, UploadRequest uploadRequest) {
        Manifest manifest;
        try {
            manifest = yamlMapper.readValue(yamlContent, Manifest.class);
            manifest.setRawYaml(yamlContent);
        } catch (JsonProcessingException e) {
            log.error("read swds yaml failed {}",fileName,e);
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("manifest parsing error"+e.getMessage()),
                HttpStatus.BAD_REQUEST);
        }
        if(null == manifest.getName() || null == manifest.getVersion()){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("name or version is required in manifest "),
                HttpStatus.BAD_REQUEST);
        }
        SWDatasetEntity swDatasetEntity = swdsMapper.findByName(manifest.getName());
        if(null == swDatasetEntity){
            //create
            swDatasetEntity = from(manifest,uploadRequest.getProject());
            swdsMapper.addDataset(swDatasetEntity);
        }
        SWDatasetVersionEntity swDatasetVersionEntity = swdsVersionMapper
            .findByDSIdAndVersionNameForUpdate(swDatasetEntity.getId(), manifest.getVersion());
        if(null == swDatasetVersionEntity){
            //create
            swDatasetVersionEntity = from(swDatasetEntity,manifest);
            swdsVersionMapper.addNewVersion(swDatasetVersionEntity);
            uploadManifest(swDatasetVersionEntity,fileName,yamlContent.getBytes(StandardCharsets.UTF_8));
        }else{
            //swds version create dup
            if(swDatasetVersionEntity.getStatus() == SWDatasetVersionEntity.STATUS_AVAILABLE){
                if(uploadRequest.force()){
                    Set<Long> runningDataSets = livingTaskCache.ofStatus(TaskStatus.RUNNING)
                        .parallelStream().map(task -> task.getJob().getSwDataSets())
                        .flatMap(Collection::stream)
                        .map(SWDataSet::getId)
                        .collect(Collectors.toSet());
                    if(runningDataSets.contains(swDatasetVersionEntity.getId())){
                        throw new SWValidationException(ValidSubject.SWDS).tip(" swds version is being hired by running job, force push is not allowed now");
                    }else {
                        swdsVersionMapper.updateStatus(swDatasetVersionEntity.getId(),SWDatasetVersionEntity.STATUS_UN_AVAILABLE);
                    }
                }else {
                    throw new SWValidationException(ValidSubject.SWDS).tip(" same swds version can't be uploaded twice");
                }

            }
            if(!yamlContent.equals(swDatasetVersionEntity.getVersionMeta())){
                swDatasetVersionEntity.setVersionMeta(yamlContent);
                //if manifest(signature) change, all files should be re-uploaded
                swDatasetVersionEntity.setFilesUploaded("");
                clearSwdsStorageData(swDatasetVersionEntity);
                swdsVersionMapper.updateFilesUploaded(swDatasetVersionEntity);
                reUploadManifest(swDatasetVersionEntity,fileName,yamlContent.getBytes(StandardCharsets.UTF_8));
            }
        }
        hotSwdsHolder.manifest(swDatasetVersionEntity);
        return swDatasetVersionEntity.getVersionName();
    }


    private SWDatasetVersionEntity from(SWDatasetEntity swDatasetEntity, Manifest manifest) {
        return SWDatasetVersionEntity.builder().datasetId(swDatasetEntity.getId())
            .ownerId(getOwner())
            .storagePath(storagePathCoordinator.generateSwdsPath(swDatasetEntity.getDatasetName(),manifest.getVersion()))
            .versionMeta(manifest.getRawYaml())
            .versionName(manifest.getVersion())
            .filesUploaded(EMPTY_YAML)
            .build();
    }

    private SWDatasetEntity from(Manifest manifest,String project) {
        ProjectEntity projectEntity = projectManager.findByName(project);
        return SWDatasetEntity.builder()
            .datasetName(manifest.getName())
            .isDeleted(0)
            .ownerId(getOwner())
            .projectId(projectEntity == null? null:projectEntity.getId())
            .build();
    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if(null == currentUserDetail){
            throw new StarWhaleApiException(new SWAuthException(SWAuthException.AuthType.SWDS_UPLOAD),HttpStatus.FORBIDDEN);
        }
        return Long.valueOf(currentUserDetail.getIdTableKey());
    }


    public void end(String uploadId) {
        final SWDSVersionWithMeta swdsVersionWithMeta = getSwdsVersion(uploadId);
        swdsVersionMapper.updateStatus(swdsVersionWithMeta.getSwDatasetVersionEntity().getId(),SWDatasetVersionEntity.STATUS_AVAILABLE);
        hotSwdsHolder.end(uploadId);
    }
}
