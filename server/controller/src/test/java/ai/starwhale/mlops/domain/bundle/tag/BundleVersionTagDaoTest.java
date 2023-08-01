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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.tag.mapper.BundleVersionTagMapper;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BundleVersionTagDaoTest {
    private BundleVersionTagMapper bundleVersionTagMapper;
    private BundleVersionTagDao bundleVersionTagDao;

    @BeforeEach
    public void setUp() {
        bundleVersionTagMapper = mock(BundleVersionTagMapper.class);
        bundleVersionTagDao = new BundleVersionTagDao(bundleVersionTagMapper);
    }

    @Test
    public void testAdd() {
        var entity = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(2L)
                .tag("tag")
                .ownerId(3L)
                .build();
        doNothing().when(bundleVersionTagMapper).add(entity);
        bundleVersionTagDao.add(BundleAccessor.Type.MODEL, 1L, 2L, "tag", 3L);
        verify(bundleVersionTagMapper).add(entity);
    }

    @Test
    public void testFindTag() {
        var entity = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(2L)
                .tag("tag")
                .build();
        when(bundleVersionTagMapper.findTag("MODEL", 1L, "tag")).thenReturn(entity);
        var get = bundleVersionTagDao.findTag(BundleAccessor.Type.MODEL, 1L, "tag");
        verify(bundleVersionTagMapper).findTag("MODEL", 1L, "tag");
        assertEquals(entity, get);
    }

    @Test
    public void testDelete() {
        doNothing().when(bundleVersionTagMapper).deleteTag("MODEL", 1L, 2L, "tag");
        bundleVersionTagDao.delete(BundleAccessor.Type.MODEL, 1L, 2L, "tag");
        verify(bundleVersionTagMapper).deleteTag("MODEL", 1L, 2L, "tag");
    }

    @Test
    public void testListByVersionIds() {
        var entity2 = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(2L)
                .tag("tag2")
                .build();
        var entity3 = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(3L)
                .tag("tag3")
                .build();

        when(bundleVersionTagMapper.listByBundleIdVersions("MODEL", 1L, "2,3")).thenReturn(List.of(entity2, entity3));
        var get = bundleVersionTagDao.listByVersionIds(BundleAccessor.Type.MODEL, 1L, List.of(2L, 3L));
        verify(bundleVersionTagMapper).listByBundleIdVersions("MODEL", 1L, "2,3");
        assertEquals(Set.of(2L, 3L), get.keySet());
        assertEquals(List.of(entity2), get.get(2L));
        assertEquals(List.of(entity3), get.get(3L));
    }

    @Test
    public void testGetJoinedTagsByVersionIds() {
        var entity2 = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(2L)
                .tag("tag2")
                .build();
        var entity3 = BundleVersionTagEntity.builder()
                .type("MODEL")
                .bundleId(1L)
                .versionId(3L)
                .tag("tag3")
                .build();

        when(bundleVersionTagMapper.listByBundleIdVersions("MODEL", 1L, "2,3")).thenReturn(List.of(entity2, entity3));
        var get = bundleVersionTagDao.getJoinedTagsByVersionIds(BundleAccessor.Type.MODEL, 1L, List.of(2L, 3L));
        verify(bundleVersionTagMapper).listByBundleIdVersions("MODEL", 1L, "2,3");
        assertEquals(Map.of(2L, "tag2", 3L, "tag3"), get);
    }
}
