/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.project.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.swds.SWDSObject.Version;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SWDatasetService {

    @Resource
    private SWDatasetMapper swdsMapper;

    @Resource
    private SWDatasetVersionMapper swdsVersionMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private SWDSConvertor swdsConvertor;

    @Resource
    private SWDSVersionConvertor versionConvertor;

    @Resource
    private ProjectManager projectManager;

    public List<DatasetVO> listSWDataset(SWDSObject swds, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetEntity> entities = swdsMapper.listDatasets(
            idConvertor.revert(swds.getProjectId()), swds.getName());

        return entities.stream()
            .map(swdsConvertor::convert)
            .collect(Collectors.toList());
    }

    public Boolean deleteSWDS(SWDSObject swds) {
        int res = swdsMapper.deleteDataset(idConvertor.revert(swds.getId()));
        return res > 0;
    }


    public Boolean modifySWDSVersion(Version version) {
        int update = swdsVersionMapper.updateVersionTag(
            SWDatasetVersionEntity.builder()
                .id(idConvertor.revert(version.getId()))
                .versionTag(version.getTag())
                .storagePath(version.getStoragePath())
                .build());
        return update > 0;
    }

    public Boolean revertVersionTo(SWDSObject swds) {
        int res = swdsVersionMapper.revertTo(idConvertor.revert(swds.getId()),
            idConvertor.revert(swds.getLatestVersion().getId()));

        return res > 0;
    }

    public List<DatasetVersionVO> listDatasetVersionHistory(SWDSObject swds, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetVersionEntity> entities = swdsVersionMapper.listVersions(
            idConvertor.revert(swds.getId()), swds.getLatestVersion().getName());

        return entities.stream()
            .map(versionConvertor::convert)
            .collect(Collectors.toList());
    }

    public String addDataset(SWDSObject swds) {
        SWDatasetEntity entity = SWDatasetEntity.builder()
            .datasetName(swds.getName())
            .ownerId(idConvertor.revert(swds.getOwnerId()))
            .projectId(idConvertor.revert(swds.getProjectId()))
            .build();
        if(entity.getProjectId() == 0) {
            ProjectEntity defaultProject = projectManager.findDefaultProject(entity.getOwnerId());
            if(defaultProject != null) {
                entity.setProjectId(defaultProject.getId());
            }
        }
        swdsMapper.addDataset(entity);
        return idConvertor.convert(entity.getId());
    }

    public String addVersion(SWDSObject swds) {
        SWDatasetVersionEntity entity = SWDatasetVersionEntity.builder()
            .datasetId(idConvertor.revert(swds.getId()))
            .ownerId(idConvertor.revert(swds.getLatestVersion().getOwnerId()))
            .versionTag(swds.getLatestVersion().getTag())
            .versionName(swds.getLatestVersion().getName())
            .versionMeta(swds.getLatestVersion().getMeta())
            .storagePath(swds.getLatestVersion().getStoragePath())
            .build();
        swdsVersionMapper.addNewVersion(entity);
        return idConvertor.convert(entity.getId());
    }

    public List<DatasetVO> findDatasetsByVersionIds(List<String> versionIds) {
        List<Long> vIds = versionIds.stream()
            .map(idConvertor::revert)
            .collect(Collectors.toList());
        List<SWDatasetVersionEntity> versions = swdsVersionMapper.findVersionsByIds(vIds);

        List<Long> ids = versions.stream()
            .map(SWDatasetVersionEntity::getDatasetId)
            .collect(Collectors.toList());

        List<SWDatasetEntity> datasets = swdsMapper.findDatasetsByIds(ids);

        return datasets.stream()
            .map(swdsConvertor::convert)
            .collect(Collectors.toList());
    }

}
