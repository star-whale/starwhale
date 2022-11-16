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

package ai.starwhale.mlops.domain.bundle.tag;

import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.util.TagUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TagManager {

    private final BundleManager bundleManager;

    private final TagAccessor tagAccessor;

    public static TagManager create(BundleManager bundleManager, TagAccessor tagAccessor) {
        return new TagManager(bundleManager, tagAccessor);
    }

    private TagManager(BundleManager bundleManager, TagAccessor tagAccessor) {
        this.bundleManager = bundleManager;
        this.tagAccessor = tagAccessor;
    }

    public Boolean updateTag(BundleVersionUrl bundleVersionUrl, TagAction tagAction) throws TagException {
        Long versionId = bundleManager.getBundleVersionId(bundleVersionUrl);
        HasTag entity = tagAccessor.findObjectWithTagById(versionId);
        if (entity == null) {
            throw new TagException(
                    String.format("Unable to find the version, url=%s ", bundleVersionUrl.getVersionUrl()));
        }
        entity.setTag(TagUtil.manageTags(tagAction, entity.getTag()));
        return tagAccessor.updateTag(entity);
    }
}
