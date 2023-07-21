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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectAccessor;
import ai.starwhale.mlops.exception.StarwhaleException;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BundleManagerTest {

    private BundleManager bundleManager;
    private ProjectAccessor projectAccessor;
    private BundleAccessor bundleAccessor;
    private BundleVersionAccessor bundleVersionAccessor;

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
        given(projectAccessor.getProjectId(anyString()))
                .willReturn(1L);
        bundleVersionAccessor = mock(BundleVersionAccessor.class);
        bundleManager = new BundleManager(
                new IdConverter(),
                new VersionAliasConverter(),
                projectAccessor,
                bundleAccessor,
                bundleVersionAccessor
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

        res = bundleManager.getBundleVersionId(
                BundleVersionUrl.create("", "", "1"), 1L);
        assertThat(res, is(1L));

        res = bundleManager.getBundleVersionId(
                BundleVersionUrl.create("", "", "v1"), 1L);
        assertThat(res, is(2L));

        res = bundleManager.getBundleVersionId(
                BundleVersionUrl.create("", "", "vName"), 1L);
        assertThat(res, is(3L));

        res = bundleManager.getBundleVersionId(
                BundleVersionUrl.create("", "", "latest"), 1L);
        assertThat(res, is(4L));

        assertThrows(StarwhaleException.class,
                () -> bundleManager.getBundleVersionId(BundleVersionUrl.create("", "", "2"), 1L));


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
