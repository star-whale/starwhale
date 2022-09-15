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

import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.BundleVersionAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.bundle.remove.RemoveAccessor;
import ai.starwhale.mlops.domain.bundle.revert.RevertAccessor;
import ai.starwhale.mlops.domain.bundle.tag.HasTag;
import ai.starwhale.mlops.domain.bundle.tag.HasTagWrapper;
import ai.starwhale.mlops.domain.bundle.tag.TagAccessor;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SwdsManager implements BundleAccessor, BundleVersionAccessor, TagAccessor,
        RevertAccessor, RecoverAccessor, RemoveAccessor {

    private final SwDatasetMapper datasetMapper;
    private final SwDatasetVersionMapper datasetVersionMapper;
    private final IdConvertor idConvertor;
    private final VersionAliasConvertor versionAliasConvertor;

    public SwdsManager(SwDatasetMapper datasetMapper, SwDatasetVersionMapper datasetVersionMapper,
            IdConvertor idConvertor, VersionAliasConvertor versionAliasConvertor) {
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public Long getSwdsVersionId(String versionUrl, Long swdsId) {
        if (idConvertor.isId(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        SwDatasetVersionEntity entity = datasetVersionMapper.findByDsIdAndVersionName(swdsId, versionUrl);
        if (entity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWMP)
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
    public BundleVersionEntity findVersionById(Long bundleVersionId) {
        return datasetVersionMapper.getVersionById(bundleVersionId);
    }

    @Override
    public BundleVersionEntity findVersionByAliasAndBundleId(String alias, Long bundleId) {
        Long versionOrder = versionAliasConvertor.revert(alias);
        return datasetVersionMapper.findByDsIdAndVersionOrder(bundleId, versionOrder);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return datasetVersionMapper.findByDsIdAndVersionName(bundleId, name);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        SwDatasetVersionEntity entity = datasetVersionMapper.getVersionById(id);
        return HasTagWrapper.builder()
                .id(entity.getId())
                .tag(entity.getVersionTag())
                .build();
    }

    @Override
    public Boolean updateTag(HasTag entity) {
        int r = datasetVersionMapper.updateTag(entity.getId(), entity.getTag());
        if (r > 0) {
            log.info("Dataset Version Tag has been modified. ID={}", entity.getId());
        }
        return r > 0;
    }

    @Override
    public Boolean revertTo(Long bundleId, Long bundleVersionId) {
        return datasetVersionMapper.revertTo(bundleId, bundleVersionId) > 0;
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

    @Override
    public Boolean remove(Long id) {
        int r = datasetMapper.deleteDataset(id);
        if (r > 0) {
            log.info("SWDS has been removed. ID={}", id);
        }
        return r > 0;
    }
}
