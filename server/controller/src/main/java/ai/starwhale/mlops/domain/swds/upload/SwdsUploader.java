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

package ai.starwhale.mlops.domain.swds.upload;

import static ai.starwhale.mlops.domain.swds.upload.SWDSVersionWithMetaConverter.EMPTY_YAML;

import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
import ai.starwhale.mlops.domain.swds.datastore.DataStoreTableNameHelper;
import ai.starwhale.mlops.domain.swds.datastore.IndexWriter;
import ai.starwhale.mlops.domain.swds.po.SWDatasetEntity;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.upload.bo.Manifest;
import ai.starwhale.mlops.domain.swds.upload.bo.SWDSVersionWithMeta;
import ai.starwhale.mlops.domain.swds.upload.bo.VersionMeta;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SWAuthException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import ai.starwhale.mlops.storage.LargeFileInputStream;
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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

    final HotJobHolder jobHolder;
    final ProjectManager projectManager;

    final DataStoreTableNameHelper dataStoreTableNameHelper;

    final IndexWriter indexWriter;

    static final String INDEX_FILE_NAME="_meta.jsonl";
    static final String AUTH_FILE_NAME="auth_env";

    public SwdsUploader(HotSwdsHolder hotSwdsHolder, SWDatasetMapper swdsMapper,
        SWDatasetVersionMapper swdsVersionMapper, StoragePathCoordinator storagePathCoordinator,
        StorageAccessService storageAccessService, UserService userService,
        @Qualifier("yamlMapper") ObjectMapper yamlMapper,
        HotJobHolder jobHolder,
        ProjectManager projectManager, DataStoreTableNameHelper dataStoreTableNameHelper,
        IndexWriter indexWriter) {
        this.hotSwdsHolder = hotSwdsHolder;
        this.swdsMapper = swdsMapper;
        this.swdsVersionMapper = swdsVersionMapper;
        this.storagePathCoordinator = storagePathCoordinator;
        this.storageAccessService = storageAccessService;
        this.userService = userService;
        this.yamlMapper = yamlMapper;
        this.jobHolder = jobHolder;
        this.projectManager = projectManager;
        this.dataStoreTableNameHelper = dataStoreTableNameHelper;
        this.indexWriter = indexWriter;
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

    public void uploadBody(String uploadId, MultipartFile file,String uri){
        final SWDSVersionWithMeta swDatasetVersionWithMeta = getSwdsVersion(uploadId);
        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()){
            if(INDEX_FILE_NAME.equals(filename)){
                try(InputStream anotherInputStream = file.getInputStream()){
                    indexWriter.writeToStore(swDatasetVersionWithMeta.getSwDatasetVersionEntity().getIndexTable(),anotherInputStream);
                }
            }
            if(AUTH_FILE_NAME.equals(filename)){
                try(InputStream anotherInputStream = file.getInputStream()){
                    swdsVersionMapper.updateStorageAuths(swDatasetVersionWithMeta.getSwDatasetVersionEntity()
                        .getId(), new String(anotherInputStream.readAllBytes()));
                }
            }
            InputStream is = inputStream;
            if(file.getSize() >= Integer.MAX_VALUE){
                is = new LargeFileInputStream(inputStream,file.getSize());
            }
            final String storagePath = String.format(FORMATTER_STORAGE_PATH, swDatasetVersionWithMeta.getSwDatasetVersionEntity().getStoragePath(),
                StringUtils.hasText(uri)?uri:filename);
            storageAccessService.put(storagePath,is);
        } catch (IOException e) {
            log.error("read swds failed {}", filename,e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.NETWORK),
                HttpStatus.INTERNAL_SERVER_ERROR);
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
        ProjectEntity projectEntity = projectManager.getProject(uploadRequest.getProject());
        Long projectId = projectEntity.getId();
        SWDatasetEntity swDatasetEntity = swdsMapper.findByName(manifest.getName(), projectId);
        if(null == swDatasetEntity){
            //create
            swDatasetEntity = from(manifest,uploadRequest.getProject());
            swdsMapper.addDataset(swDatasetEntity);
        }
        SWDatasetVersionEntity swDatasetVersionEntity = swdsVersionMapper
            .findByDSIdAndVersionNameForUpdate(swDatasetEntity.getId(), manifest.getVersion());
        if(null == swDatasetVersionEntity){
            //create
            swDatasetVersionEntity = from(projectEntity.getProjectName(),swDatasetEntity,manifest);
            swdsVersionMapper.addNewVersion(swDatasetVersionEntity);
            uploadManifest(swDatasetVersionEntity,fileName,yamlContent.getBytes(StandardCharsets.UTF_8));
        }else{
            //swds version create dup
            if(swDatasetVersionEntity.getStatus().equals(SWDatasetVersionEntity.STATUS_AVAILABLE)){
                if(uploadRequest.force()){
                    Set<Long> runningDataSets = jobHolder.ofStatus(Set.of(JobStatus.RUNNING))
                        .parallelStream().map(Job::getSwDataSets)
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


    private SWDatasetVersionEntity from(String projectName,SWDatasetEntity swDatasetEntity, Manifest manifest) {
        return SWDatasetVersionEntity.builder().datasetId(swDatasetEntity.getId())
            .ownerId(getOwner())
            .storagePath(storagePathCoordinator.generateSwdsPath(projectName,swDatasetEntity.getDatasetName(),manifest.getVersion()))
            .versionMeta(manifest.getRawYaml())
            .versionName(manifest.getVersion())
            .size(manifest.getDatasetSummary().getRows())
            .indexTable(dataStoreTableNameHelper.tableNameOfDataset(projectName,manifest.getName(),manifest.getVersion()))
            .filesUploaded(EMPTY_YAML)
            .build();
    }

    private SWDatasetEntity from(Manifest manifest,String project) {
        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project, userService.currentUserDetail().getIdTableKey());
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
        return currentUserDetail.getIdTableKey();
    }


    public void end(String uploadId) {
        final SWDSVersionWithMeta swdsVersionWithMeta = getSwdsVersion(uploadId);
        swdsVersionMapper.updateStatus(swdsVersionWithMeta.getSwDatasetVersionEntity().getId(),SWDatasetVersionEntity.STATUS_AVAILABLE);
        hotSwdsHolder.end(uploadId);
    }

    final static String SWDS_MANIFEST="_manifest.yaml";

    public void pull(String project, String name, String version, String partName, HttpServletResponse httpResponse) {
        Long projectId = projectManager.getProject(project).getId();
        SWDatasetEntity datasetEntity = swdsMapper.findByName(name, projectId);
        if(null == datasetEntity){
            throw new SWValidationException(ValidSubject.SWDS).tip("dataset name doesn't exists");
        }
        SWDatasetVersionEntity datasetVersionEntity = swdsVersionMapper.findByDSIdAndVersionName(
            datasetEntity.getId(), version);
        if(null == datasetVersionEntity){
            throw new SWValidationException(ValidSubject.SWDS).tip("dataset version doesn't exists");
        }
        if(!StringUtils.hasText(partName)){
            partName = SWDS_MANIFEST;
        }
        try(InputStream inputStream = storageAccessService.get(datasetVersionEntity.getStoragePath() + "/" + partName.trim());
            ServletOutputStream outputStream = httpResponse.getOutputStream()){
            long length = inputStream.transferTo(outputStream);
            httpResponse.addHeader("Content-Disposition","attachment; filename=\""+fileNameFromUri(partName)+"\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("pull file from storage failed",e);
            throw new SWProcessException(ErrorType.STORAGE).tip("pull file from storage failed: "+ e.getMessage());
        }
    }

    String fileNameFromUri(String uri){
        String[] split = uri.split("/");
        return split[split.length-1];
    }
}
