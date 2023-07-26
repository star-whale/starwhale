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

package ai.starwhale.mlops.domain.runtime;

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
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RuntimeDao implements BundleAccessor, BundleVersionAccessor, TagAccessor,
        RevertAccessor, RecoverAccessor, RemoveAccessor {

    private final RuntimeMapper runtimeMapper;
    private final RuntimeVersionMapper runtimeVersionMapper;
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;

    public RuntimeDao(RuntimeMapper runtimeMapper, RuntimeVersionMapper runtimeVersionMapper,
            IdConverter idConvertor, VersionAliasConverter versionAliasConvertor) {
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
    }

    public RuntimeEntity getRuntime(Long id) {
        RuntimeEntity runtime = runtimeMapper.find(id);

        if (runtime == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE,
                    String.format("Unable to find Runtime %s", id));
        }
        return runtime;
    }

    public RuntimeEntity getRuntimeByName(String name, Long projectId) {
        return runtimeMapper.findByName(name, projectId, false);
    }

    public RuntimeVersionEntity getRuntimeVersion(Long runtimeId, String version) {
        RuntimeVersionEntity entity = runtimeVersionMapper.findByNameAndRuntimeId(version, runtimeId);
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                    String.format("Unable to find Runtime Version %s", version));
        }
        return entity;
    }

    public RuntimeVersionEntity getRuntimeVersion(String versionUrl) {
        RuntimeVersionEntity entity;
        if (idConvertor.isId(versionUrl)) {
            var id = idConvertor.revert(versionUrl);
            entity = runtimeVersionMapper.find(id);
        } else {
            // TODO need deprecated, it's not unique
            entity = runtimeVersionMapper.findByNameAndRuntimeId(versionUrl, null);
        }
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION,
                    String.format("Unable to find Runtime Version %s", versionUrl));
        }
        return entity;
    }

    public boolean updateVersionBuiltImage(String version, String image) {
        return runtimeVersionMapper.updateBuiltImage(version, image) > 0;
    }

    @Override
    public BundleEntity findById(Long id) {
        return runtimeMapper.find(id);
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return runtimeMapper.findByName(name, projectId, true);
    }

    @Override
    public BundleVersionEntity findVersionById(Long bundleVersionId) {
        return runtimeVersionMapper.find(bundleVersionId);
    }

    @Override
    public BundleVersionEntity findVersionByAliasAndBundleId(String alias, Long bundleId) {
        Long versionOrder = versionAliasConvertor.revert(alias);
        return runtimeVersionMapper.findByVersionOrder(versionOrder, bundleId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return runtimeVersionMapper.findByNameAndRuntimeId(name, bundleId);
    }

    @Override
    public BundleVersionEntity findLatestVersionByBundleId(Long bundleId) {
        return runtimeVersionMapper.findByLatest(bundleId);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        RuntimeVersionEntity entity = runtimeVersionMapper.find(id);
        return HasTagWrapper.builder()
                .id(entity.getId())
                .tag(entity.getVersionTag())
                .build();
    }

    @Override
    public Boolean updateTag(HasTag entity) {
        int r = runtimeVersionMapper.updateTag(entity.getId(), entity.getTag());
        if (r > 0) {
            log.info("Runtime Version Tag has been modified. ID={}", entity.getId());
        }
        return r > 0;
    }

    @Override
    public Long selectVersionOrderForUpdate(Long bundleId, Long bundleVersionId) {
        return runtimeVersionMapper.selectVersionOrderForUpdate(bundleVersionId);
    }

    @Override
    public Long selectMaxVersionOrderOfBundleForUpdate(Long bundleId) {
        return runtimeVersionMapper.selectMaxVersionOrderOfRuntimeForUpdate(bundleId);
    }

    @Override
    public int updateVersionOrder(Long versionId, Long versionOrder) {
        return runtimeVersionMapper.updateVersionOrder(versionId, versionOrder);
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return runtimeMapper.findDeleted(id);
    }

    @Override
    public Boolean recover(Long id) {
        return runtimeMapper.recover(id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        int r = runtimeMapper.remove(id);
        if (r > 0) {
            log.info("SWRT has been removed. ID={}", id);
        }
        return r > 0;
    }
}
