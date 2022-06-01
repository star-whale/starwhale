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
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
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

    public PageInfo<DatasetVO> listSWDataset(SWDSObject swds, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetEntity> entities = swdsMapper.listDatasets(
            swds.getProjectId(), swds.getName());

        return PageUtil.toPageInfo(entities, ds -> {
            SWDatasetVersionEntity version = swdsVersionMapper.getLatestVersion(ds.getId());
            DatasetVO vo = swdsConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        });
    }

    public Boolean deleteSWDS(SWDSObject swds) {
        Long id = swds.getId();
        int res = swdsMapper.deleteDataset(id);
        log.info("SWDS has been deleted. ID={}", swds.getId());
        return res > 0;
    }

    public SWDatasetInfoVO getSWDSInfo(SWDSObject swds) {
        Long dsID = swds.getId();
        SWDatasetEntity ds = swdsMapper.findDatasetById(dsID);
        if(ds == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip("Unable to find swds " + dsID), HttpStatus.BAD_REQUEST);
        }

        SWDatasetVersionEntity versionEntity = swdsVersionMapper.getLatestVersion(dsID);
        if(versionEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip("Unable to find the latest version of swds " + ds.getId()), HttpStatus.BAD_REQUEST);
        }
        return toSWDatasetInfoVO(ds, versionEntity);

    }

    private SWDatasetInfoVO toSWDatasetInfoVO(SWDatasetEntity ds,SWDatasetVersionEntity versionEntity) {

        //Get file list in storage
        try {
            String storagePath = versionEntity.getStoragePath();
            List<StorageFileVO> collect = storageService.listStorageFile(storagePath);

            return SWDatasetInfoVO.builder()
                .swdsName(ds.getDatasetName())
                .versionName(versionEntity.getVersionName())
                .versionTag(versionEntity.getVersionTag())
                .versionMeta(versionEntity.getVersionMeta())
                .files(collect)
                .build();

        } catch (IOException e) {
            log.error("list swds storage", e);
            throw new StarWhaleApiException(new SWProcessException(ErrorType.STORAGE)
                .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public Boolean modifySWDSVersion(SWDSVersion version) {
        SWDatasetVersionEntity entity = SWDatasetVersionEntity.builder()
            .id(version.getId())
            .versionTag(version.getTag())
            .build();
        int update = swdsVersionMapper.update(entity);
        log.info("SWDS Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public Boolean revertVersionTo(SWDSObject swds) {
        Long swdsId = swds.getId();
        Long revertTo = swds.getCurrentVersion().getId();
        int res = swdsVersionMapper.revertTo(swdsId, revertTo);
        log.info("SWDS Version {} has been revert to {}", swdsId, revertTo);
        return res > 0;
    }

    public PageInfo<DatasetVersionVO> listDatasetVersionHistory(SWDSObject swds, SWDSVersion version, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetVersionEntity> entities = swdsVersionMapper.listVersions(
            swds.getId(), version.getName(), version.getTag());
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
            SWDatasetEntity ds = swdsMapper.findByName(name);
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
}
