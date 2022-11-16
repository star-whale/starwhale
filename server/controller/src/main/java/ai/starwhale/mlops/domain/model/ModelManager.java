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
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModelManager implements BundleAccessor, BundleVersionAccessor, TagAccessor,
        RevertAccessor, RecoverAccessor, RemoveAccessor {

    private final ModelMapper modelMapper;
    private final ModelVersionMapper versionMapper;
    private final IdConvertor idConvertor;
    private final VersionAliasConvertor versionAliasConvertor;

    public ModelManager(ModelMapper modelMapper, ModelVersionMapper versionMapper,
            IdConvertor idConvertor, VersionAliasConvertor versionAliasConvertor) {
        this.modelMapper = modelMapper;
        this.versionMapper = versionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public Long getModelVersionId(String versionUrl, Long modelId) {
        if (idConvertor.isId(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        ModelVersionEntity entity = versionMapper.findByNameAndModelId(versionUrl, modelId);
        if (entity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.MODEL, String.format("Unable to find model %s", versionUrl)),
                    HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    @Override
    public BundleEntity findById(Long id) {
        return modelMapper.findModelById(id);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        ModelVersionEntity entity = versionMapper.findVersionById(id);
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
    public BundleEntity findByName(String name, Long projectId) {
        return modelMapper.findByName(name, projectId);
    }

    @Override
    public BundleVersionEntity findVersionById(Long bundleVersionId) {
        return versionMapper.findVersionById(bundleVersionId);
    }

    @Override
    public BundleVersionEntity findVersionByAliasAndBundleId(String alias, Long bundleId) {
        Long versionOrder = versionAliasConvertor.revert(alias);
        return versionMapper.findByVersionOrderAndModelId(versionOrder, bundleId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return versionMapper.findByNameAndModelId(name, bundleId);
    }

    @Override
    public BundleVersionEntity findLatestVersionByBundleId(Long bundleId) {
        return versionMapper.getLatestVersion(bundleId);
    }

    @Override
    public Boolean revertTo(Long bundleId, Long bundleVersionId) {
        return versionMapper.revertTo(bundleId, bundleVersionId) > 0;
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return modelMapper.findDeletedModelById(id);
    }

    @Override
    public List<? extends BundleEntity> listDeletedBundlesByName(String name, Long projectId) {
        return modelMapper.listDeletedModel(name, projectId);
    }

    @Override
    public Boolean recover(Long id) {
        return modelMapper.recoverModel(id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        int r = modelMapper.deleteModel(id);
        if (r > 0) {
            log.info("Model has been removed. ID={}", id);
        }
        return r > 0;
    }
}
