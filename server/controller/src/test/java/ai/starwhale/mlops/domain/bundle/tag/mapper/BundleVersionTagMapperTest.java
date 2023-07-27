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

package ai.starwhale.mlops.domain.bundle.tag.mapper;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BundleVersionTagMapperTest extends MySqlContainerHolder {
    @Autowired
    private BundleVersionTagMapper bundleVersionTagMapper;

    @Test
    public void testAddAndGet() {
        var entity = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(2L)
                .tag("tag")
                .ownerId(1L)
                .build();

        bundleVersionTagMapper.add(entity);

        assertThrows(DataIntegrityViolationException.class, () -> {
            // try insert with the same type + bundleId + tag
            var entity2 = BundleVersionTagEntity.builder()
                    .type("MODEL")
                    .bundleId(1L)
                    .versionId(12L)
                    .tag("tag")
                    .ownerId(11L)
                    .build();
            bundleVersionTagMapper.add(entity2);
        });

        var theSameVersionIdEntity = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(2L)
                .tag("tag2")
                .ownerId(3L)
                .build();
        bundleVersionTagMapper.add(theSameVersionIdEntity);

        var theSameBundleIdEntity = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(3L)
                .tag("tag3")
                .ownerId(4L)
                .build();
        bundleVersionTagMapper.add(theSameBundleIdEntity);

        var find = bundleVersionTagMapper.findTag("MODEL", 1L, "tag");
        // the created time is set by database, so we need to set it to the same value
        entity.setCreatedTime(find.getCreatedTime());
        assertEquals(entity, find);

        find = bundleVersionTagMapper.findTag("MODEL", 1L, "tag2");
        theSameVersionIdEntity.setCreatedTime(find.getCreatedTime());
        assertEquals(theSameVersionIdEntity, find);

        find = bundleVersionTagMapper.findTag("MODEL", 1L, "tag3");
        theSameBundleIdEntity.setCreatedTime(find.getCreatedTime());
        assertEquals(theSameBundleIdEntity, find);

        var tags = bundleVersionTagMapper.listByBundleIdVersions("MODEL", 1L, "3");
        assertThat(tags).containsExactly(theSameBundleIdEntity);

        tags = bundleVersionTagMapper.listByBundleIdVersions("MODEL", 1L, "2,3");
        assertThat(tags).containsExactlyInAnyOrder(entity, theSameVersionIdEntity, theSameBundleIdEntity);

        tags = bundleVersionTagMapper.listByVersionId("MODEL", 1L, 2L);
        assertThat(tags).containsExactlyInAnyOrder(entity, theSameVersionIdEntity);

        tags = bundleVersionTagMapper.listByBundleId("MODEL", 1L);
        assertThat(tags).containsExactlyInAnyOrder(entity, theSameVersionIdEntity, theSameBundleIdEntity);

        bundleVersionTagMapper.deleteTagById(entity.getId());
        tags = bundleVersionTagMapper.listByBundleId("MODEL", 1L);
        assertThat(tags).containsExactlyInAnyOrder(theSameVersionIdEntity, theSameBundleIdEntity);

        bundleVersionTagMapper.deleteTag("MODEL", 1L, 3L, "tag3");
        tags = bundleVersionTagMapper.listByBundleId("MODEL", 1L);
        assertThat(tags).containsExactlyInAnyOrder(theSameVersionIdEntity);

        bundleVersionTagMapper.deleteAllTags("MODEL", 1L);
        tags = bundleVersionTagMapper.listByBundleId("MODEL", 1L);
        assertThat(tags).isEmpty();
    }
}
