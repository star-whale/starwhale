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

package ai.starwhale.mlops.domain.dataset;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
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
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.job.storage.JobDatasetVersionMapper;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DatasetDao implements BundleAccessor, BundleVersionAccessor, TagAccessor,
        RevertAccessor, RecoverAccessor, RemoveAccessor {

    private final DatasetMapper datasetMapper;
    private final DatasetVersionMapper datasetVersionMapper;

    private final JobDatasetVersionMapper jobDatasetVersionMapper;
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;

    public DatasetDao(DatasetMapper datasetMapper, DatasetVersionMapper datasetVersionMapper,
            JobDatasetVersionMapper jobDatasetVersionMapper,
            IdConverter idConvertor, VersionAliasConverter versionAliasConvertor) {
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.jobDatasetVersionMapper = jobDatasetVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public Long getDatasetVersionId(String versionUrl, Long datasetId) {
        if (idConvertor.isId(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        DatasetVersionEntity entity = datasetVersionMapper.findByNameAndDatasetId(versionUrl, datasetId, false);
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                    String.format("Unable to find Dataset Version %s", versionUrl));
        }
        return entity.getId();
    }

    public DatasetVersionEntity getDatasetVersion(String versionUrl) {
        DatasetVersionEntity entity;
        if (idConvertor.isId(versionUrl)) {
            entity = datasetVersionMapper.find(idConvertor.revert(versionUrl));
        } else {
            entity = datasetVersionMapper.findByNameAndDatasetId(versionUrl, null, false);
        }
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                    String.format("Unable to find Dataset Version %s", versionUrl));
        }
        return entity;
    }

    public DatasetVersion getDatasetVersion(Long versionId) {
        DatasetVersionEntity versionEntity = datasetVersionMapper.find(versionId);
        if (null == versionEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, "Can not find dataset version" + versionId);
        }
        DatasetEntity datasetEntity = datasetMapper.find(versionEntity.getDatasetId());
        if (null == datasetEntity) {
            throw new SwNotFoundException(ResourceType.BUNDLE,
                    "Can not find dataset" + versionEntity.getDatasetId());
        }
        return DatasetVersion.fromEntity(datasetEntity, versionEntity);
    }

    @Deprecated
    public List<DatasetVersion> listDatasetVersionsOfJob(String jobId) {
        List<Long> versionIds = jobDatasetVersionMapper.listDatasetVersionIdsByJobId(jobId);
        return this.listDatasetVersions(versionIds);
    }

    public List<DatasetVersion> listDatasetVersions(List<Long> versionIds) {
        return versionIds.stream().map(this::getDatasetVersion).collect(Collectors.toList());
    }

    @Override
    public BundleEntity findById(Object id) {
        return datasetMapper.find((Long) id);
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return datasetMapper.findByName(name, projectId, true);
    }

    @Override
    public BundleVersionEntity findVersionById(Long bundleVersionId) {
        return datasetVersionMapper.find(bundleVersionId);
    }

    @Override
    public BundleVersionEntity findVersionByAliasAndBundleId(String alias, Long bundleId) {
        Long versionOrder = versionAliasConvertor.revert(alias);
        return datasetVersionMapper.findByVersionOrder(versionOrder, bundleId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return datasetVersionMapper.findByNameAndDatasetId(name, bundleId, false);
    }

    @Override
    public BundleVersionEntity findLatestVersionByBundleId(Long bundleId) {
        return datasetVersionMapper.findByLatest(bundleId);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        DatasetVersionEntity entity = datasetVersionMapper.find(id);
        return HasTagWrapper.builder()
                .id(entity.getId())
                .tag(entity.getVersionTag())
                .build();
    }

    @Override
    public Boolean updateTag(HasTag entity) {
        int r = datasetVersionMapper.updateTag((Long) entity.getId(), entity.getTag());
        if (r > 0) {
            log.info("Dataset Version Tag has been modified. ID={}", entity.getId());
        }
        return r > 0;
    }

    @Override
    public Long selectVersionOrderForUpdate(Long bundleId, Long bundleVersionId) {
        return datasetVersionMapper.selectVersionOrderForUpdate(bundleVersionId);
    }

    @Override
    public Long selectMaxVersionOrderOfBundleForUpdate(Long bundleId) {
        return datasetVersionMapper.selectMaxVersionOrderOfDatasetForUpdate(bundleId);
    }

    @Override
    public int updateVersionOrder(Long versionId, Long versionOrder) {
        return datasetVersionMapper.updateVersionOrder(versionId, versionOrder);
    }

    @Override
    public BundleEntity findDeletedBundleById(Object id) {
        return datasetMapper.findDeleted((Long) id);
    }

    @Override
    public Boolean recover(Object id) {
        return datasetMapper.recover((Long) id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        int r = datasetMapper.remove(id);
        if (r > 0) {
            log.info("Dataset has been removed. ID={}", id);
        }
        return r > 0;
    }
}
