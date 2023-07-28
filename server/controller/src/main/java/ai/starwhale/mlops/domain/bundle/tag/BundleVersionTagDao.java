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


import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.HasId;
import ai.starwhale.mlops.domain.bundle.tag.mapper.BundleVersionTagMapper;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BundleVersionTagDao {
    private final BundleVersionTagMapper bundleVersionTagMapper;

    public BundleVersionTagDao(BundleVersionTagMapper bundleVersionTagMapper) {
        this.bundleVersionTagMapper = bundleVersionTagMapper;
    }

    public void add(BundleAccessor.Type type, Long bundleId, Long versionId, String tag, Long userId) {
        var entity = BundleVersionTagEntity.builder()
                .type(type.name())
                .bundleId(bundleId)
                .versionId(versionId)
                .tag(tag)
                .ownerId(userId)
                .build();
        bundleVersionTagMapper.add(entity);
    }


    public BundleVersionTagEntity findTag(BundleAccessor.Type type, Long bundleId, String tag) {
        return bundleVersionTagMapper.findTag(type.name(), bundleId, tag);
    }

    public void delete(BundleAccessor.Type type, Long bundleId, Long versionId, String tag) {
        // versionId here is for validation
        bundleVersionTagMapper.deleteTag(type.name(), bundleId, versionId, tag);
    }

    public Map<Long, List<BundleVersionTagEntity>> listByVersionIds(
            BundleAccessor.Type type,
            Long bundleId,
            List<Long> versionIds
    ) {
        var ids = versionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        var entities = bundleVersionTagMapper.listByBundleIdVersions(type.name(), bundleId, ids);
        return entities.stream().collect(Collectors.groupingBy(BundleVersionTagEntity::getVersionId));
    }

    public Map<Long, String> getJoinedTagsByVersionIds(BundleAccessor.Type type, Long bundleId, List<Long> versionIds) {
        var tags = listByVersionIds(type, bundleId, versionIds);
        // make versionId as key, joined tags with "," as value
        return tags.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                i -> i.getValue().stream().map(BundleVersionTagEntity::getTag).collect(Collectors.joining(","))));
    }

    public Map<Long, String> getJoinedTagsByBundleVersions(
            BundleAccessor.Type type,
            Long bundleId,
            List<? extends HasId> versionIds
    ) {
        var ids = versionIds.stream().map(HasId::getId).collect(Collectors.toList());
        return getJoinedTagsByVersionIds(type, bundleId, ids);
    }
}
