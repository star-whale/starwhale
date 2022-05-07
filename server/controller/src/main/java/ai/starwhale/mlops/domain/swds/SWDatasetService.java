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
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.ProjectEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.swds.SWDSObject.Version;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
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

    public PageInfo<DatasetVO> listSWDataset(SWDSObject swds, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetEntity> entities = swdsMapper.listDatasets(
            idConvertor.revert(swds.getProjectId()), swds.getName());

        return PageUtil.toPageInfo(entities, swdsConvertor::convert);
    }

    public Boolean deleteSWDS(SWDSObject swds) {
        Long id = idConvertor.revert(swds.getId());
        int res = swdsMapper.deleteDataset(id);
        log.info("SWDS has been deleted. ID={}", swds.getId());
        return res > 0;
    }

    public DatasetVersionVO getSWDSInfo(SWDSObject swds) {
        Long dsID = idConvertor.revert(swds.getId());

        SWDatasetVersionEntity entity = swdsVersionMapper.getLatestVersion(dsID);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip("Unable to find the latest version of swmp " + dsID), HttpStatus.BAD_REQUEST);
        }

        return versionConvertor.convert(entity);
    }


    public Boolean modifySWDSVersion(Version version) {
        SWDatasetVersionEntity entity = SWDatasetVersionEntity.builder()
            .id(idConvertor.revert(version.getId()))
            .versionTag(version.getTag())
            .storagePath(version.getStoragePath())
            .build();
        int update = swdsVersionMapper.update(entity);
        log.info("SWDS Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public Boolean revertVersionTo(SWDSObject swds) {
        Long swdsId = idConvertor.revert(swds.getId());
        Long revertTo = idConvertor.revert(swds.getLatestVersion().getId());
        int res = swdsVersionMapper.revertTo(swdsId, revertTo);
        log.info("SWDS Version {} has been revert to {}", swdsId, revertTo);
        return res > 0;
    }

    public PageInfo<DatasetVersionVO> listDatasetVersionHistory(SWDSObject swds, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWDatasetVersionEntity> entities = swdsVersionMapper.listVersions(
            idConvertor.revert(swds.getId()), swds.getLatestVersion().getName());
        return PageUtil.toPageInfo(entities, versionConvertor::convert);
    }

    public String addDataset(SWDSObject swds) {
        SWDatasetEntity entity = SWDatasetEntity.builder()
            .datasetName(swds.getName())
            .ownerId(idConvertor.revert(swds.getOwnerId()))
            .projectId(idConvertor.revert(swds.getProjectId()))
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
        log.info("SWDS Version has been created. DSID={}, VID={}", entity.getDatasetId(), entity.getId());
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
