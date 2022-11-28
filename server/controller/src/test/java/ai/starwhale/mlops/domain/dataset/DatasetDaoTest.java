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

package ai.starwhale.mlops.domain.dataset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.bundle.tag.HasTagWrapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatasetDaoTest {

    private DatasetDao manager;
    private DatasetMapper datasetMapper;
    private DatasetVersionMapper versionMapper;

    @BeforeEach
    public void setUp() {
        datasetMapper = mock(DatasetMapper.class);
        versionMapper = mock(DatasetVersionMapper.class);
        manager = new DatasetDao(
                datasetMapper,
                versionMapper,
                new IdConverter(),
                new VersionAliasConverter()
        );
    }

    @Test
    public void testFindById() {
        given(datasetMapper.find(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return DatasetEntity.builder().id(id).build();
                });
        var res = manager.findById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindObjectWithTagById() {
        given(versionMapper.find(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return DatasetVersionEntity.builder().id(id).versionTag("tag").build();
                });

        var res = manager.findObjectWithTagById(1L);
        assertThat(res, allOf(
                hasProperty("id", is(1L)),
                hasProperty("tag", is("tag"))
        ));

        res = manager.findObjectWithTagById(2L);
        assertThat(res, allOf(
                hasProperty("id", is(2L)),
                hasProperty("tag", is("tag"))
        ));
    }

    @Test
    public void testUpdateTag() {
        given(versionMapper.updateTag(same(1L), same("tag")))
                .willReturn(1);
        var res = manager.updateTag(HasTagWrapper.builder().id(1L).tag("tag").build());
        assertThat(res, is(true));
    }

    @Test
    public void testFindByName() {
        given(datasetMapper.findByName(same("d1"), same(1L), any()))
                .willReturn(DatasetEntity.builder().id(1L).build());

        var res = manager.findByNameForUpdate("d1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findByNameForUpdate("m2", 1L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindVersionById() {
        given(versionMapper.find(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return DatasetVersionEntity.builder().id(id).build();
                });

        var res = manager.findVersionById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindVersionByAliasAndBundleId() {
        given(versionMapper.findByVersionOrder(anyLong(), anyLong()))
                .willReturn(DatasetVersionEntity.builder()
                        .id(1L)
                        .versionName("dataset1")
                        .build());
        var res = manager.findVersionByAliasAndBundleId("v1", 1L);
        assertThat(res, allOf(
                notNullValue(),
                isA(DatasetVersionEntity.class),
                hasProperty("id", is(1L)),
                hasProperty("versionName", is("dataset1"))
        ));
    }

    @Test
    public void testFindVersionByNameAndBundleId() {
        given(versionMapper.findByNameAndDatasetId(same("d1"), same(1L), any()))
                .willReturn(DatasetVersionEntity.builder().id(1L).build());

        var res = manager.findVersionByNameAndBundleId("d1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionByNameAndBundleId("d2", 1L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindLatestVersionByBundleId() {
        given(versionMapper.findByLatest(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).build());

        var res = manager.findLatestVersionByBundleId(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findLatestVersionByBundleId(2L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindDeletedBundleById() {
        given(datasetMapper.findDeleted(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return DatasetEntity.builder().id(id).build();
                });

        var res = manager.findDeletedBundleById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findDeletedBundleById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testRevertTo() {

    }

    @Test
    public void testRecover() {
        given(datasetMapper.recover(same(1L)))
                .willReturn(1);

        var res = manager.recover(1L);
        assertThat(res, is(true));

        res = manager.recover(2L);
        assertThat(res, is(false));
    }

    @Test
    public void testRemove() {
        given(datasetMapper.remove(same(1L)))
                .willReturn(1);

        var res = manager.remove(1L);
        assertThat(res, is(true));

        res = manager.remove(2L);
        assertThat(res, is(false));
    }
}
