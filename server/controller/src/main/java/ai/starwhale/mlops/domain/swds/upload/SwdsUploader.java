/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.SWDatasetEntity;
import ai.starwhale.mlops.domain.swds.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionMapper;
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
import java.util.Optional;

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

    /**
     * prefix + / + fileName
     */
    static final String FORMATTER_STORAGE_PATH="%s/%s";

    final ObjectMapper yamlMapper;

    public SwdsUploader(HotSwdsHolder hotSwdsHolder, SWDatasetMapper swdsMapper, SWDatasetVersionMapper swdsVersionMapper, StoragePathCoordinator storagePathCoordinator, StorageAccessService storageAccessService, UserService userService, @Qualifier("yamlMapper") ObjectMapper yamlMapper) {
        this.hotSwdsHolder = hotSwdsHolder;
        this.swdsMapper = swdsMapper;
        this.swdsVersionMapper = swdsVersionMapper;
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.userService = userService;
        this.yamlMapper = yamlMapper;
    }

    public void cancel(String uploadId){
        final SWDatasetVersionEntity swDatasetVersionEntity = getSwdsVersion(uploadId);
        swdsVersionMapper.deleteById(Long.valueOf(uploadId));
        hotSwdsHolder.cancel(Long.valueOf(uploadId));
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
            log.error("delete storage objects failed for {}",uploadId,e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public void uploadBody(String uploadId, MultipartFile file){
        final SWDatasetVersionEntity swDatasetVersionEntity = getSwdsVersion(uploadId);
        final String storagePath = String.format(FORMATTER_STORAGE_PATH,swDatasetVersionEntity.getStoragePath(),file.getName());
        try(final InputStream inputStream = file.getInputStream()){
            storageAccessService.put(storagePath,inputStream);
        } catch (IOException e) {
            log.error("upload swds to failed {}",file.getName(),e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    SWDatasetVersionEntity getSwdsVersion(String uploadId) {
        final Optional<SWDatasetVersionEntity> swDatasetVersionEntityOpt = hotSwdsHolder.of(Long.valueOf(uploadId));
        return swDatasetVersionEntityOpt
            .orElseThrow(
                () -> new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS),
                    HttpStatus.BAD_REQUEST));
    }

    void uploadManifest(SWDatasetVersionEntity swDatasetVersionEntity,String fileName, byte[] bytes){
        final String storagePath = String.format(FORMATTER_STORAGE_PATH,swDatasetVersionEntity.getStoragePath(),fileName);
        try{
            storageAccessService.put(storagePath,bytes);
        } catch (IOException e) {
            log.error("upload swds to failed {}",fileName,e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @Transactional
    public String create(String yamlContent,String fileName) {
        Manifest manifest;
        try {
            manifest = yamlMapper.readValue(yamlContent, Manifest.class);
            manifest.setRawYaml(yamlContent);
        } catch (JsonProcessingException e) {
            log.error("read swds yaml failed {}",fileName,e);
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("manifest parsing error"+e.getMessage()),
                HttpStatus.BAD_REQUEST);
        }
        SWDatasetEntity swDatasetEntity = swdsMapper.findByName(manifest.getName());
        if(null == swDatasetEntity){
            //create
            swDatasetEntity = from(manifest);
            swdsMapper.addDataset(from(manifest));

        }
        SWDatasetVersionEntity byDSIdAndVersionName = swdsVersionMapper
            .findByDSIdAndVersionNameForUpdate(swDatasetEntity.getId(), manifest.getVersion());
        if(null == byDSIdAndVersionName){
            //create
            byDSIdAndVersionName = from(swDatasetEntity,manifest);
            swdsVersionMapper.addNewVersion(byDSIdAndVersionName);
        }else{
            //swds version create dup
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS).tip("swds version duplicated "+manifest.getVersion()),
                HttpStatus.BAD_REQUEST);
        }
        uploadManifest(byDSIdAndVersionName,fileName,yamlContent.getBytes(StandardCharsets.UTF_8));
        hotSwdsHolder.manifest(byDSIdAndVersionName);

        return byDSIdAndVersionName.getId().toString();
    }

    private SWDatasetVersionEntity from(SWDatasetEntity swDatasetEntity, Manifest manifest) {
        return SWDatasetVersionEntity.builder().datasetId(swDatasetEntity.getId())
            .ownerId(getOwner())
            .storagePath(storagePathCoordinator.swdsPath(swDatasetEntity.getId().toString(),manifest.getVersion()))
            .versionMeta(manifest.getRawYaml())
            .versionName(manifest.getVersion())
            .build();
    }

    private SWDatasetEntity from(Manifest manifest) {
        return SWDatasetEntity.builder()
            .datasetName(manifest.getName())
            .isDeleted(0)
            .ownerId(getOwner())
            .build();
    }

    private Long getOwner() {
        User currentUserDetail = userService.currentUserDetail();
        if(null == currentUserDetail){
            throw new SWAuthException(SWAuthException.AuthType.SWDS_UPLOAD);
        }
        return Long.valueOf(currentUserDetail.getIdTableKey());
    }


    public void end(String uploadId) {
        final SWDatasetVersionEntity swDatasetVersionEntity = getSwdsVersion(uploadId);
        swdsVersionMapper.updateStatus(swDatasetVersionEntity.getId(),SWDatasetVersionEntity.STATUS_AVAILABLE);
        hotSwdsHolder.end(Long.valueOf(uploadId));
    }
}
