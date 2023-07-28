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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import ai.starwhale.mlops.domain.project.ProjectAccessor;
import ai.starwhale.mlops.exception.StarwhaleException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BundleManagerTest {

    private BundleManager bundleManager;
    private ProjectAccessor projectAccessor;
    private BundleAccessor bundleAccessor;
    private BundleVersionAccessor bundleVersionAccessor;
    private BundleVersionTagDao bundleVersionTagDao;

    @BeforeEach
    public void setUp() {
        projectAccessor = mock(ProjectAccessor.class);
        given(projectAccessor.getProjectId(anyString()))
                .willReturn(1L);
        bundleAccessor = mock(BundleAccessor.class);
        given(bundleAccessor.findById(same(1L)))
                .willReturn(EntityWrapper.builder().id(1L).build());
        given(bundleAccessor.findByNameForUpdate(same("bundle1"), anyLong()))
                .willReturn(EntityWrapper.builder().id(2L).name("bundle1").build());
        given(bundleAccessor.getType()).willReturn(BundleAccessor.Type.JOB);
        given(projectAccessor.getProjectId(anyString()))
                .willReturn(1L);
        bundleVersionAccessor = mock(BundleVersionAccessor.class);
        bundleVersionTagDao = mock(BundleVersionTagDao.class);
        bundleManager = new BundleManager(
                new IdConverter(),
                new VersionAliasConverter(),
                projectAccessor,
                bundleAccessor,
                bundleVersionAccessor,
                bundleVersionTagDao
        );
    }

    @Test
    public void testGetBundleId() {
        var res = bundleManager.getBundleId(BundleUrl.create("1", "1"));
        assertThat(res, is(1L));

        res = bundleManager.getBundleId(BundleUrl.create("1", "bundle1"));
        assertThat(res, is(2L));

        assertThrows(StarwhaleException.class,
                () -> bundleManager.getBundleId(BundleUrl.create("1", "bundle2")));

    }

    @Test
    public void testGetBundleVersionId() {
        given(bundleVersionAccessor.findVersionById(same(1L)))
                .willReturn(VersionEntityWrapper.builder().id(1L).name("byId").build());
        given(bundleVersionAccessor.findVersionByAliasAndBundleId(anyString(), anyLong()))
                .willReturn(VersionEntityWrapper.builder().id(2L).name("byAlias").build());
        given(bundleVersionAccessor.findVersionByNameAndBundleId(anyString(), anyLong()))
                .willReturn(VersionEntityWrapper.builder().id(3L).name("byName").build());
        given(bundleVersionAccessor.findLatestVersionByBundleId(anyLong()))
                .willReturn(VersionEntityWrapper.builder().id(4L).name("byLatest").build());

        var res = bundleManager.getBundleVersionId(
                BundleVersionUrl.create("1", "1", "1"));
        assertThat(res, is(1L));

        res = bundleManager.getBundleVersionId(1L, "1");
        assertThat(res, is(1L));

        res = bundleManager.getBundleVersionId(1L, "v1");
        assertThat(res, is(2L));

        res = bundleManager.getBundleVersionId(1L, "vName");
        assertThat(res, is(3L));

        res = bundleManager.getBundleVersionId(1L, "latest");
        assertThat(res, is(4L));

        assertThrows(StarwhaleException.class,
                () -> bundleManager.getBundleVersionId(1L, "2"));
    }

    @Test
    public void testGetBundleVersion() {
        // test getting by tag
        var tagEntity = BundleVersionTagEntity.builder()
                .type(BundleAccessor.Type.JOB.name())
                .bundleId(1L)
                .versionId(2L)
                .tag("tag1")
                .build();
        given(bundleVersionTagDao.findTag(BundleAccessor.Type.JOB, 2L, "tag1")).willReturn(tagEntity);
        given(bundleVersionAccessor.findVersionById(2L))
                .willReturn(VersionEntityWrapper.builder().id(2L).build());
        var spyBundleManager = spy(bundleManager);
        doReturn(2L).when(spyBundleManager).getBundleId(BundleUrl.create("1", "2"));
        var version = spyBundleManager.getBundleVersion(BundleVersionUrl.create("1", "2", "tag1"));
        assertThat(version.getId(), is(2L));
    }

    @Test
    public void testBundleVersionTag() {
        var spyBundleManager = spy(bundleManager);
        doReturn(2L).when(spyBundleManager).getBundleId(BundleUrl.create("1", "2"));
        doReturn(3L).when(spyBundleManager).getBundleVersionId(2L, "3");

        doNothing().when(bundleVersionTagDao).add(BundleAccessor.Type.JOB, 2L, 3L, "tag1", 4L);
        spyBundleManager.addBundleVersionTag(BundleAccessor.Type.JOB, "1", "2", "3", "tag1", 4L);
        verify(bundleVersionTagDao).add(BundleAccessor.Type.JOB, 2L, 3L, "tag1", 4L);

        doNothing().when(bundleVersionTagDao).delete(eq(BundleAccessor.Type.JOB), eq(2L), eq(3L), eq("tag1"));
        spyBundleManager.deleteBundleVersionTag(BundleAccessor.Type.JOB, "1", "2", "3", "tag1");
        verify(bundleVersionTagDao).delete(eq(BundleAccessor.Type.JOB), eq(2L), eq(3L), eq("tag1"));

        var tagEntity = BundleVersionTagEntity.builder()
                .type(BundleAccessor.Type.JOB.name())
                .bundleId(2L)
                .versionId(3L)
                .tag("tag1")
                .build();
        given(bundleVersionTagDao.listByVersionIds(BundleAccessor.Type.JOB, 2L, List.of(3L)))
                .willReturn(Map.of(3L, List.of(tagEntity)));

        var res = spyBundleManager.listBundleVersionTags(BundleAccessor.Type.JOB, "1", "2", "3");
        assertThat(res.size(), is(1));
        assertThat(res.get(0), is("tag1"));
    }

    @Data
    @Builder
    private static class EntityWrapper implements BundleEntity {

        private Long id;
        private String name;
        private Long projectId;

        private Date modifiedTime;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Date getModifiedTime() {
            return modifiedTime;
        }
    }

    @Data
    @Builder
    private static class VersionEntityWrapper implements BundleVersionEntity {

        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
