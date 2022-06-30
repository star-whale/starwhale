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

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.api.protocol.StorageFileVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.api.protocol.swds.SWDatasetInfoVO;
import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.common.util.TagUtil;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swds.bo.SWDSObject;
import ai.starwhale.mlops.domain.swds.bo.SWDSQuery;
import ai.starwhale.mlops.domain.swds.bo.SWDSVersion;
import ai.starwhale.mlops.domain.swds.bo.SWDSVersionQuery;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SWDatasetEntity;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SWDatasetService {

    @Resource
    private SWDatasetMapper swdsMapper;

    @Resource
    private SWDatasetVersionMapper swdsVersionMapper;

    @Resource
    private SWDSConvertor swdsConvertor;

    @Resource
    private SWDSVersionConvertor versionConvertor;

    @Resource
    private StorageService storageService;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private SwdsManager swdsManager;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    public PageInfo<DatasetVO> listSWDataset(SWDSQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<SWDatasetEntity> entities = swdsMapper.listDatasets(projectId,
            query.getNamePrefix());

        return PageUtil.toPageInfo(entities, ds -> {
            SWDatasetVersionEntity version = swdsVersionMapper.getLatestVersion(ds.getId());
            DatasetVO vo = swdsConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        });
    }

    public Boolean deleteSWDS(SWDSQuery query) {
        Long id = swdsManager.getSWDSId(query.getSwdsUrl(), query.getProjectUrl());
        int res = swdsMapper.deleteDataset(id);
        log.info("SWDS has been deleted. ID={}", id);
        return res > 0;
    }

    public Boolean recoverSWDS(String projectUrl, String datasetUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        String name = datasetUrl;
        Long id;
        if(idConvertor.isID(datasetUrl)) {
            id = idConvertor.revert(datasetUrl);
            SWDatasetEntity entity = swdsMapper.findDeletedDatasetById(id);
            if(entity == null) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                    .tip("Recover dataset error. Dataset can not be found. "), HttpStatus.BAD_REQUEST);
            }
            name = entity.getDatasetName();
        } else {
            // To restore datasets by name, need to check whether there are duplicate names
            List<SWDatasetEntity> deletedDatasets = swdsMapper.listDeletedDatasets(name, projectId);
            if(deletedDatasets.size() > 1) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                    .tip(String.format("Recover dataset error. Duplicate names [%s] of deleted dataset. ", name)),
                    HttpStatus.BAD_REQUEST);
            } else if (deletedDatasets.size() == 0) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                    .tip(String.format("Recover dataset error. Can not find deleted dataset [%s].", name)),
                    HttpStatus.BAD_REQUEST);
            }
            id = deletedDatasets.get(0).getId();
        }

        // Check for duplicate names
        if(swdsMapper.findByName(name, projectId) != null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip(String.format("Recover dataset error. Model %s already exists", name)), HttpStatus.BAD_REQUEST);
        }

        int res = swdsMapper.recoverDataset(id);
        log.info("Dataset has been recovered. Name={}", name);
        return res > 0;
    }

    public SWDatasetInfoVO getSWDSInfo(SWDSQuery query) {
        Long datasetId = swdsManager.getSWDSId(query.getSwdsUrl(), query.getProjectUrl());
        SWDatasetEntity ds = swdsMapper.findDatasetById(datasetId);
        if(ds == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip("Unable to find swds " + query.getSwdsUrl()), HttpStatus.BAD_REQUEST);
        }

        SWDatasetVersionEntity versionEntity = null;
        if(!StrUtil.isEmpty(query.getSwdsVersionUrl())) {
            Long versionId = swdsManager.getSWDSVersionId(query.getSwdsVersionUrl(), ds.getId());
            versionEntity = swdsVersionMapper.getVersionById(versionId);
        }
        if(versionEntity == null) {
            versionEntity = swdsVersionMapper.getLatestVersion(ds.getId());
        }
        if(versionEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip("Unable to find the latest version of swds " + query.getSwdsUrl()), HttpStatus.BAD_REQUEST);
        }
        return toSWDatasetInfoVO(ds, versionEntity);

    }

    private SWDatasetInfoVO toSWDatasetInfoVO(SWDatasetEntity ds,SWDatasetVersionEntity versionEntity) {

        //Get file list in storage
        try {
            String storagePath = versionEntity.getStoragePath();
            List<StorageFileVO> collect = storageService.listStorageFile(storagePath);

            return SWDatasetInfoVO.builder()
                .id(idConvertor.convert(ds.getId()))
                .name(ds.getDatasetName())
                .versionName(versionEntity.getVersionName())
                .versionTag(versionEntity.getVersionTag())
                .versionMeta(versionEntity.getVersionMeta())
                .createdTime(localDateTimeConvertor.convert(versionEntity.getCreatedTime()))
                .files(collect)
                .build();

        } catch (IOException e) {
            log.error("list swds storage", e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE)
                .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public Boolean modifySWDSVersion(String projectUrl, String swdsUrl, String versionUrl, SWDSVersion version) {
        Long swdsId = swdsManager.getSWDSId(swdsUrl, projectUrl);
        Long versionId = swdsManager.getSWDSVersionId(versionUrl, swdsId);
        SWDatasetVersionEntity entity = SWDatasetVersionEntity.builder()
            .id(versionId)
            .versionTag(version.getTag())
            .build();
        int update = swdsVersionMapper.update(entity);
        log.info("SWDS Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public Boolean manageVersionTag(String projectUrl, String datasetUrl, String versionUrl,
        TagAction tagAction) {
        Long id = swdsManager.getSWDSId(datasetUrl, projectUrl);
        Long versionId = swdsManager.getSWDSVersionId(versionUrl, id);

        SWDatasetVersionEntity entity = swdsVersionMapper.getVersionById(versionId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip("Unable to find the version of swds " + versionUrl), HttpStatus.BAD_REQUEST);
        }
        entity.setVersionTag(TagUtil.getTags(tagAction, entity.getVersionTag()));
        int update = swdsVersionMapper.update(entity);
        log.info("SWDS Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public Boolean revertVersionTo(String projectUrl, String swdsUrl, String versionUrl) {
        Long id = swdsManager.getSWDSId(swdsUrl, projectUrl);
        Long vid = swdsManager.getSWDSVersionId(versionUrl, id);
        int res = swdsVersionMapper.revertTo(id, vid);
        log.info("SWDS Version has been revert to {}" , vid);
        return res > 0;
    }

    public PageInfo<DatasetVersionVO> listDatasetVersionHistory(SWDSVersionQuery query, PageParams pageParams) {
        Long swdsId = swdsManager.getSWDSId(query.getSwdsUrl(), query.getProjectUrl());
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetVersionEntity> entities = swdsVersionMapper.listVersions(
            swdsId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, versionConvertor::convert);
    }

    public Long addDataset(SWDSObject swds) {
        SWDatasetEntity entity = SWDatasetEntity.builder()
            .datasetName(swds.getName())
            .ownerId(swds.getOwnerId())
            .projectId(swds.getProjectId())
            .build();
        if(entity.getProjectId() == 0) {
            ProjectEntity defaultProject = projectManager.findDefaultProject();
            if(defaultProject == null) {
                throw new StarWhaleApiException(new SWProcessException(ErrorType.DB)
                    .tip("Unable to find default project by user " + entity.getOwnerId()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            entity.setProjectId(defaultProject.getId());
        }
        swdsMapper.addDataset(entity);
        log.info("SWDS has been created. ID={}", entity.getId());
        return entity.getId();
    }

    public Long addVersion(SWDSObject swds) {
        SWDatasetVersionEntity entity = SWDatasetVersionEntity.builder()
            .datasetId(swds.getId())
            .ownerId(swds.getCurrentVersion().getOwnerId())
            .versionTag(swds.getCurrentVersion().getTag())
            .versionName(swds.getCurrentVersion().getName())
            .versionMeta(swds.getCurrentVersion().getMeta())
            .storagePath(swds.getCurrentVersion().getStoragePath())
            .build();
        swdsVersionMapper.addNewVersion(entity);
        log.info("SWDS Version has been created. DSID={}, VID={}", entity.getDatasetId(), entity.getId());
        return entity.getId();
    }

    public List<DatasetVO> findDatasetsByVersionIds(List<Long> versionIds) {
        List<SWDatasetVersionEntity> versions = swdsVersionMapper.findVersionsByIds(versionIds);

        return versions.stream().map(version -> {
            SWDatasetEntity ds = swdsMapper.findDatasetById(version.getDatasetId());
            DatasetVO vo = swdsConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<SWDatasetInfoVO> listDS(String project, String name) {
        if(StringUtils.hasText(name)){
            Long projectId = projectManager.getProjectId(project);
            SWDatasetEntity ds = swdsMapper.findByName(name, projectId);
            if(null == ds){
                throw new SWValidationException(ValidSubject.SWDS)
                    .tip("Unable to find the swds with name " + name);
            }
            return swDatasetInfoOfDs(ds);
        }
        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project);

        List<SWDatasetEntity> swDatasetEntities = swdsMapper.listDatasets(projectEntity.getId(), null);
        if(null == swDatasetEntities || swDatasetEntities.isEmpty()){
            return List.of();
        }
        return swDatasetEntities.parallelStream()
            .map(this::swDatasetInfoOfDs)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<SWDatasetInfoVO> swDatasetInfoOfDs(SWDatasetEntity ds) {
        List<SWDatasetVersionEntity> swDatasetVersionEntities = swdsVersionMapper.listVersions(
            ds.getId(), null, null);
        if(null == swDatasetVersionEntities || swDatasetVersionEntities.isEmpty()){
            return List.of();
        }
        return swDatasetVersionEntities.parallelStream()
            .map(entity -> toSWDatasetInfoVO(ds, entity)).collect(Collectors.toList());
    }

    public String query(UploadRequest uploadRequest) {
        Long projectId = projectManager.getProjectId(uploadRequest.getProject());
        SWDatasetEntity entity = swdsMapper.findByName(uploadRequest.name(), projectId);
        if(null == entity) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS), HttpStatus.NOT_FOUND);
        }
        SWDatasetVersionEntity versionEntity = swdsVersionMapper.findByDSIdAndVersionName(entity.getId(), uploadRequest.version());
        if(null == versionEntity) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS), HttpStatus.NOT_FOUND);
        }
        return "";
    }
}
