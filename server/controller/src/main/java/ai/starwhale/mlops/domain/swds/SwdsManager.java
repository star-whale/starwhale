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

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.BundleVersionAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.bundle.revert.RevertAccessor;
import ai.starwhale.mlops.domain.bundle.tag.TagAccessor;
import ai.starwhale.mlops.domain.bundle.tag.HasTag;
import ai.starwhale.mlops.domain.bundle.tag.HasTagWrapper;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SWDatasetEntity;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SwdsManager implements BundleAccessor, BundleVersionAccessor, TagAccessor,
    RevertAccessor, RecoverAccessor {

    @Resource
    private SWDatasetMapper datasetMapper;

    @Resource
    private SWDatasetVersionMapper datasetVersionMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private ProjectManager projectManager;

    public Long getSWDSId(String swdsUrl, String projectUrl) {
        if(idConvertor.isID(swdsUrl)) {
            return idConvertor.revert(swdsUrl);
        }
        Long projectId = projectManager.getProjectId(projectUrl);
        SWDatasetEntity entity = datasetMapper.findByName(swdsUrl, projectId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWDS)
                .tip(String.format("Unable to find swds %s", swdsUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    public Long getSWDSVersionId(String versionUrl, Long swdsId) {
        if(idConvertor.isID(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        SWDatasetVersionEntity entity = datasetVersionMapper.findByDSIdAndVersionName(swdsId, versionUrl);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find Runtime %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    @Override
    public BundleEntity findById(Long id) {
        return datasetMapper.findDatasetById(id);
    }

    @Override
    public BundleEntity findByName(String name, Long projectId) {
        return datasetMapper.findByName(name, projectId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return datasetVersionMapper.findByDSIdAndVersionName(bundleId, name);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        SWDatasetVersionEntity entity = datasetVersionMapper.getVersionById(id);
        return HasTagWrapper.builder()
            .id(entity.getId())
            .tag(entity.getVersionTag())
            .build();
    }

    @Override
    public int updateTag(HasTag entity) {
        return datasetVersionMapper.updateTag(entity.getId(), entity.getTag());
    }

    @Override
    public int revertTo(Long bundleId, Long bundleVersionId) {
        return datasetVersionMapper.revertTo(bundleId, bundleVersionId);
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return datasetMapper.findDeletedDatasetById(id);
    }

    @Override
    public List<? extends BundleEntity> listDeletedBundlesByName(String name, Long projectId) {
        return datasetMapper.listDeletedDatasets(name, projectId);
    }

    @Override
    public Boolean recover(Long id) {
        return datasetMapper.recoverDataset(id) > 0;
    }
}
