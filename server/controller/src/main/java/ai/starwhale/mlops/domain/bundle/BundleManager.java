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

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import ai.starwhale.mlops.domain.project.ProjectAccessor;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class BundleManager {

    public static final String BUNDLE_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{2,80}$";
    // any character a-z, A-Z or 0-9, and _ or - or . and length 2-80, can not be "latest" nor v{number} nor pure number
    private static final Pattern BUNDLE_TAG_PATTERN = Pattern.compile("^(?!latest$)(?!v\\d+$)(?!\\d+$)[\\w._-]{2,80}$");

    private final ProjectAccessor projectAccessor;
    private final IdConverter idConvertor;
    private final VersionAliasConverter versionAliasConvertor;
    private final BundleAccessor bundleAccessor;
    private final BundleVersionAccessor bundleVersionAccessor;
    private final BundleVersionTagDao bundleVersionTagDao;

    public BundleManager(
            IdConverter idConvertor,
            VersionAliasConverter versionAliasConvertor,
            ProjectAccessor projectAccessor,
            BundleAccessor bundleAccessor,
            BundleVersionAccessor bundleVersionAccessor,
            BundleVersionTagDao bundleVersionTagDao
    ) {
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.projectAccessor = projectAccessor;
        this.bundleAccessor = bundleAccessor;
        this.bundleVersionAccessor = bundleVersionAccessor;
        this.bundleVersionTagDao = bundleVersionTagDao;
    }

    public Long getBundleId(BundleUrl bundleUrl) {
        return getBundleId(bundleUrl.getBundleUrl(), bundleUrl.getProjectUrl());
    }

    private Long getBundleId(String bundleUrl, String projectUrl) throws BundleException {
        return getBundle(bundleUrl, projectUrl).getId();
    }

    public BundleEntity getBundle(BundleUrl bundleUrl) throws BundleException {
        return getBundle(bundleUrl.getBundleUrl(), bundleUrl.getProjectUrl());
    }

    private BundleEntity getBundle(String bundleUrl, String projectUrl) throws BundleException {
        BundleEntity entity;
        if (idConvertor.isId(bundleUrl)) {
            entity = bundleAccessor.findById(idConvertor.revert(bundleUrl));
        } else {
            Long projectId = projectAccessor.getProjectId(projectUrl);
            entity = bundleAccessor.findByNameForUpdate(bundleUrl, projectId);
        }
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE, String.format("Unable to find %s", bundleUrl));
        }
        return entity;
    }

    public Long getBundleVersionId(BundleVersionUrl bundleVersionUrl) {
        Long bundleId = getBundleId(bundleVersionUrl.getBundleUrl());
        return getBundleVersionId(bundleId, bundleVersionUrl.getVersionUrl());
    }

    public Long getBundleVersionId(Long bundleId, String versionUrl) {
        return getBundleVersion(versionUrl, bundleId).getId();
    }

    public BundleVersionEntity getBundleVersion(BundleVersionUrl bundleVersionUrl) {
        Long bundleId = getBundleId(bundleVersionUrl.getBundleUrl());
        return getBundleVersion(bundleVersionUrl.getVersionUrl(), bundleId);
    }

    private BundleVersionEntity getBundleVersion(String versionUrl, Long bundleId) {
        BundleVersionEntity entity;
        if (idConvertor.isId(versionUrl)) {
            entity = bundleVersionAccessor.findVersionById(idConvertor.revert(versionUrl));
        } else if (versionAliasConvertor.isVersionAlias(versionUrl)) {
            entity = bundleVersionAccessor.findVersionByAliasAndBundleId(versionUrl, bundleId);
        } else if (versionAliasConvertor.isLatest(versionUrl)) {
            entity = bundleVersionAccessor.findLatestVersionByBundleId(bundleId);
        } else {
            entity = bundleVersionAccessor.findVersionByNameAndBundleId(versionUrl, bundleId);
            if (entity == null) {
                // find by tag
                var tag = bundleVersionTagDao.findTag(bundleAccessor.getType(), bundleId, versionUrl);
                if (tag != null) {
                    entity = bundleVersionAccessor.findVersionById(tag.getVersionId());
                }
            }
        }
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.BUNDLE_VERSION, String.format("Unable to find %s", versionUrl));
        }
        return entity;
    }

    @Transactional
    public synchronized void addBundleVersionTag(
            BundleAccessor.Type type,
            String projectUrl,
            String bundleUrl,
            String bundleVersionUrl,
            String tag,
            Long userId,
            Boolean force
    ) {
        if (!BUNDLE_TAG_PATTERN.matcher(tag).matches()) {
            throw new SwValidationException(SwValidationException.ValidSubject.TAG,
                    String.format("Invalid tag %s, must match %s", tag, BUNDLE_TAG_PATTERN.pattern())
            );
        }
        var bundleId = getBundleId(BundleUrl.create(projectUrl, bundleUrl));
        var versionId = getBundleVersionId(bundleId, bundleVersionUrl);
        if (Boolean.TRUE.equals(force) && bundleVersionTagDao.findTag(type, bundleId, tag) != null) {
            bundleVersionTagDao.delete(type, bundleId, versionId, tag);
        }

        bundleVersionTagDao.add(type, bundleId, versionId, tag, userId);
    }

    public List<String> listBundleVersionTags(
            BundleAccessor.Type type,
            String projectUrl,
            String modelUrl,
            String versionUrl
    ) {
        var bundleId = getBundleId(BundleUrl.create(projectUrl, modelUrl));
        var versionId = getBundleVersionId(bundleId, versionUrl);
        var tags = bundleVersionTagDao.listByVersionIds(type, bundleId, List.of(versionId));
        return tags.get(versionId).stream()
                .map(BundleVersionTagEntity::getTag)
                .collect(Collectors.toList());
    }

    public void deleteBundleVersionTag(
            BundleAccessor.Type type,
            String projectUrl,
            String modelUrl,
            String versionUrl,
            String tag
    ) {
        var bundleId = getBundleId(BundleUrl.create(projectUrl, modelUrl));
        var versionId = getBundleVersionId(bundleId, versionUrl);
        bundleVersionTagDao.delete(type, bundleId, versionId, tag);
    }

    public BundleVersionTagEntity getBundleVersionTag(
            BundleAccessor.Type type,
            String projectUrl,
            String modelUrl,
            String tag
    ) {
        var bundleId = getBundleId(BundleUrl.create(projectUrl, modelUrl));
        return bundleVersionTagDao.findTag(type, bundleId, tag);
    }
}
