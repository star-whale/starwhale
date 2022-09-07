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

import ai.starwhale.mlops.common.IdConvertor;
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
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RuntimeManager implements BundleAccessor, BundleVersionAccessor, TagAccessor,
        RevertAccessor, RecoverAccessor, RemoveAccessor {

    @Resource
    private RuntimeMapper runtimeMapper;
    @Resource
    private RuntimeVersionMapper runtimeVersionMapper;
    @Resource
    private IdConvertor idConvertor;

    public Long getRuntimeVersionId(String versionUrl, Long runtimeId) {
        if (idConvertor.isId(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        RuntimeVersionEntity entity = runtimeVersionMapper.findByNameAndRuntimeId(versionUrl, runtimeId);
        if (entity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.RUNTIME)
                    .tip(String.format("Unable to find Runtime %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    @Override
    public BundleEntity findById(Long id) {
        return runtimeMapper.findRuntimeById(id);
    }

    @Override
    public BundleEntity findByName(String name, Long projectId) {
        return runtimeMapper.findByName(name, projectId);
    }

    @Override
    public BundleVersionEntity findVersionById(Long bundleVersionId) {
        return runtimeVersionMapper.findVersionById(bundleVersionId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return runtimeVersionMapper.findByNameAndRuntimeId(name, bundleId);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        RuntimeVersionEntity entity = runtimeVersionMapper.findVersionById(id);
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
    public Boolean revertTo(Long bundleId, Long bundleVersionId) {
        log.info("Runtime Version {} has been revert to {}", bundleId, bundleVersionId);
        return runtimeVersionMapper.revertTo(bundleId, bundleVersionId) > 0;
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return runtimeMapper.findDeletedRuntimeById(id);
    }

    @Override
    public List<? extends BundleEntity> listDeletedBundlesByName(String name, Long projectId) {
        return runtimeMapper.listDeletedRuntimes(name, projectId);
    }

    @Override
    public Boolean recover(Long id) {
        return runtimeMapper.recoverRuntime(id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        int r = runtimeMapper.deleteRuntime(id);
        if (r > 0) {
            log.info("SWRT has been removed. ID={}", id);
        }
        return r > 0;
    }
}
