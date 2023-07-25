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

package ai.starwhale.mlops.domain.model;

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
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModelDao implements BundleAccessor, BundleVersionAccessor, TagAccessor,
        RevertAccessor, RecoverAccessor, RemoveAccessor {

    private final ModelMapper modelMapper;
    private final ModelVersionMapper versionMapper;
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;

    public ModelDao(ModelMapper modelMapper, ModelVersionMapper versionMapper,
            IdConverter idConvertor, VersionAliasConverter versionAliasConvertor) {
        this.modelMapper = modelMapper;
        this.versionMapper = versionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public ModelEntity getModel(Long id) {
        var model =  modelMapper.find(id);
        if (model == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE,
                String.format("Unable to find Model id %s", id));
        }
        return model;
    }

    public ModelVersionEntity getModelVersion(String versionUrl) {
        ModelVersionEntity entity;
        if (idConvertor.isId(versionUrl)) {
            var id = idConvertor.revert(versionUrl);
            entity = versionMapper.find(id);
        } else {
            entity = versionMapper.findByNameAndModelId(versionUrl, null);
        }

        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                    String.format("Unable to find Model Version %s", versionUrl));
        }
        return entity;
    }

    @Override
    public BundleEntity findById(Long id) {
        return modelMapper.find(id);
    }

    @Override
    public BundleEntity findByUrl(String url) {
        if (idConvertor.isId(url)) {
            return modelMapper.find(idConvertor.revert(url));
        }
        return modelMapper.findByNameOnly(url);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        ModelVersionEntity entity = versionMapper.find(id);
        return HasTagWrapper.builder()
                .id(entity.getId())
                .tag(entity.getVersionTag())
                .build();
    }

    @Override
    public Boolean updateTag(HasTag entity) {
        int r = versionMapper.updateTag(entity.getId(), entity.getTag());
        if (r > 0) {
            log.info("Model Version Tag has been modified. ID={}", entity.getId());
        }
        return r > 0;
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return modelMapper.findByName(name, projectId, true);
    }

    @Override
    public BundleVersionEntity findVersionById(Long bundleVersionId) {
        return versionMapper.find(bundleVersionId);
    }

    @Override
    public BundleVersionEntity findVersionByAliasAndBundleId(String alias, Long bundleId) {
        Long versionOrder = versionAliasConvertor.revert(alias);
        return versionMapper.findByVersionOrder(versionOrder, bundleId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return versionMapper.findByNameAndModelId(name, bundleId);
    }

    @Override
    public BundleVersionEntity findLatestVersionByBundleId(Long bundleId) {
        return versionMapper.findByLatest(bundleId);
    }


    @Override
    public Long selectVersionOrderForUpdate(Long bundleId, Long bundleVersionId) {
        return versionMapper.selectVersionOrderForUpdate(bundleVersionId);
    }

    @Override
    public Long selectMaxVersionOrderOfBundleForUpdate(Long bundleId) {
        return versionMapper.selectMaxVersionOrderOfModelForUpdate(bundleId);
    }

    @Override
    public int updateVersionOrder(Long versionId, Long versionOrder) {
        return versionMapper.updateVersionOrder(versionId, versionOrder);
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return modelMapper.findDeleted(id);
    }

    @Override
    public Boolean recover(Long id) {
        return modelMapper.recover(id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        int r = modelMapper.remove(id);
        if (r > 0) {
            log.info("Model has been removed. ID={}", id);
        }
        return r > 0;
    }
}
