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

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.BundleVersionAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.bundle.remove.RemoveAccessor;
import ai.starwhale.mlops.domain.bundle.revert.RevertAccessor;
import ai.starwhale.mlops.domain.bundle.tag.TagAccessor;
import ai.starwhale.mlops.domain.bundle.tag.HasTag;
import ai.starwhale.mlops.domain.bundle.tag.HasTagWrapper;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SwmpManager implements BundleAccessor, BundleVersionAccessor, TagAccessor,
    RevertAccessor, RecoverAccessor, RemoveAccessor {

    @Resource
    private SWModelPackageMapper swmpMapper;
    @Resource
    private SWModelPackageVersionMapper versionMapper;
    @Resource
    private IDConvertor idConvertor;

    @Resource
    private ProjectManager projectManager;

    public Long getSWMPId(String swmpUrl, String projectUrl) {
        if(idConvertor.isID(swmpUrl)) {
            return idConvertor.revert(swmpUrl);
        }

        Long projectId = projectManager.getProjectId(projectUrl);
        SWModelPackageEntity entity = swmpMapper.findByName(swmpUrl, projectId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find swmp %s", swmpUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    public Long getSWMPVersionId(String versionUrl, Long swmpId) {
        if(idConvertor.isID(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        SWModelPackageVersionEntity entity = versionMapper.findByNameAndSwmpId(versionUrl, swmpId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.SWMP)
                .tip(String.format("Unable to find swmp %s", versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    @Override
    public BundleEntity findById(Long id) {
        return swmpMapper.findSWModelPackageById(id);
    }

    @Override
    public HasTag findObjectWithTagById(Long id) {
        SWModelPackageVersionEntity entity = versionMapper.findVersionById(id);
        return HasTagWrapper.builder()
            .id(entity.getId())
            .tag(entity.getVersionTag())
            .build();
    }

    @Override
    public Boolean updateTag(HasTag entity) {
        int r = versionMapper.updateTag(entity.getId(), entity.getTag());
        if(r > 0) {
            log.info("Model Version Tag has been modified. ID={}", entity.getId());
        }
        return r > 0;
    }

    @Override
    public BundleEntity findByName(String name, Long projectId) {
        return swmpMapper.findByName(name, projectId);
    }

    @Override
    public BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId) {
        return versionMapper.findByNameAndSwmpId(name, bundleId);
    }

    @Override
    public Boolean revertTo(Long bundleId, Long bundleVersionId) {
        return versionMapper.revertTo(bundleVersionId, bundleVersionId) > 0;
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return swmpMapper.findDeletedSWModelPackageById(id);
    }

    @Override
    public List<? extends BundleEntity> listDeletedBundlesByName(String name, Long projectId) {
        return swmpMapper.listDeletedSWModelPackages(name, projectId);
    }

    @Override
    public Boolean recover(Long id) {
        return swmpMapper.recoverSWModelPackage(id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        int r = swmpMapper.deleteSWModelPackage(id);
        if(r > 0) {
            log.info("SWMP has been removed. ID={}", id);
        }
        return r > 0;
    }
}
