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

package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectAccessor;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class BundleManager {

    public static final String BUNDLE_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{2,80}$";
    private final ProjectAccessor projectAccessor;
    private final IdConvertor idConvertor;
    private final VersionAliasConvertor versionAliasConvertor;
    private final BundleAccessor bundleAccessor;
    private final BundleVersionAccessor bundleVersionAccessor;
    private final ValidSubject validSubject;

    public BundleManager(IdConvertor idConvertor,
            VersionAliasConvertor versionAliasConvertor,
            ProjectAccessor projectAccessor,
            BundleAccessor bundleAccessor,
            BundleVersionAccessor bundleVersionAccessor,
            ValidSubject validSubject) {
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.projectAccessor = projectAccessor;
        this.bundleAccessor = bundleAccessor;
        this.bundleVersionAccessor = bundleVersionAccessor;
        this.validSubject = validSubject;
    }

    public Long getBundleId(BundleUrl bundleUrl) {
        return getBundleId(bundleUrl.getBundleUrl(), bundleUrl.getProjectUrl());
    }

    private Long getBundleId(String bundleUrl, String projectUrl) {
        BundleEntity entity;
        if (idConvertor.isId(bundleUrl)) {
            entity = bundleAccessor.findById(idConvertor.revert(bundleUrl));
        } else {
            Long projectId = projectAccessor.getProjectId(projectUrl);
            entity = bundleAccessor.findByName(bundleUrl, projectId);
        }
        if (entity == null) {
            throw new StarwhaleApiException(new SwValidationException(validSubject)
                    .tip(String.format("Unable to find %s %s", validSubject.name(), bundleUrl)),
                    HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    public Long getBundleVersionId(BundleVersionUrl bundleVersionUrl) {
        Long bundleId = getBundleId(bundleVersionUrl.getBundleUrl());
        return getBundleVersionId(bundleVersionUrl, bundleId);
    }

    public Long getBundleVersionId(BundleVersionUrl bundleVersionUrl, Long bundleId) {
        return getBundleVersionId(bundleVersionUrl.getVersionUrl(), bundleId);
    }

    private Long getBundleVersionId(String versionUrl, Long bundleId) {
        BundleVersionEntity entity;
        if (idConvertor.isId(versionUrl)) {
            entity = bundleVersionAccessor.findVersionById(idConvertor.revert(versionUrl));
        } else if (versionAliasConvertor.isVersionAlias(versionUrl)) {
            entity = bundleVersionAccessor.findVersionByAliasAndBundleId(versionUrl, bundleId);
        } else {
            entity = bundleVersionAccessor.findVersionByNameAndBundleId(versionUrl, bundleId);
        }
        if (entity == null) {
            throw new StarwhaleApiException(new SwValidationException(validSubject)
                    .tip(String.format("Unable to find %s %s", validSubject.name(), versionUrl)),
                    HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
