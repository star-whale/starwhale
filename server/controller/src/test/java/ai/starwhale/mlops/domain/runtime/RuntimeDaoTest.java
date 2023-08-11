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

package ai.starwhale.mlops.domain.runtime;

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
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuntimeDaoTest {

    private RuntimeDao manager;
    private RuntimeMapper runtimeMapper;
    private RuntimeVersionMapper versionMapper;

    @BeforeEach
    public void setUp() {
        runtimeMapper = mock(RuntimeMapper.class);
        versionMapper = mock(RuntimeVersionMapper.class);
        manager = new RuntimeDao(
                runtimeMapper,
                versionMapper,
                new IdConverter(),
                new VersionAliasConverter()
        );
    }

    @Test
    public void testFindById() {
        given(runtimeMapper.find(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return RuntimeEntity.builder().id(id).build();
                });
        var res = manager.findById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindByName() {
        given(runtimeMapper.findByName(same("m1"), same(1L), any()))
                .willReturn(RuntimeEntity.builder().id(1L).build());

        var res = manager.findByNameForUpdate("m1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findByNameForUpdate("m2", 1L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindVersionById() {
        given(versionMapper.find(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return RuntimeVersionEntity.builder().id(id).build();
                });

        var res = manager.findVersionById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindVersionByAliasAndBundleId() {
        given(versionMapper.findByVersionOrder(anyLong(), anyLong()))
                .willReturn(RuntimeVersionEntity.builder()
                        .id(1L)
                        .versionName("runtime1")
                        .build());
        var res = manager.findVersionByAliasAndBundleId("v1", 1L);
        assertThat(res, allOf(
                notNullValue(),
                isA(RuntimeVersionEntity.class),
                hasProperty("id", is(1L)),
                hasProperty("versionName", is("runtime1"))
        ));
    }

    @Test
    public void testFindVersionByNameAndBundleId() {
        given(versionMapper.findByNameAndRuntimeId(same("r1"), same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).build());

        var res = manager.findVersionByNameAndBundleId("r1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionByNameAndBundleId("r2", 1L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindLatestVersionByBundleId() {
        given(versionMapper.findByLatest(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).build());

        var res = manager.findLatestVersionByBundleId(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findLatestVersionByBundleId(2L);
        assertThat(res, nullValue());
    }

    @Test
    public void testRevertTo() {
        Long bundleId = 1L;
        Long versionId = 5L;
        Long maxOrder = 7L;
        given(versionMapper.selectVersionOrderForUpdate(same(versionId)))
                .willReturn(2L);

        given(versionMapper.selectMaxVersionOrderOfRuntimeForUpdate(same(bundleId)))
                .willReturn(maxOrder);

        given(versionMapper.updateVersionOrder(same(versionId), same(maxOrder)))
                .willReturn(1);

        var order = manager.selectVersionOrderForUpdate(bundleId, versionId);
        assertThat(order, is(2L));

        var max = manager.selectMaxVersionOrderOfBundleForUpdate(bundleId);
        assertThat(max, is(maxOrder));

        var res = manager.updateVersionOrder(versionId, max);
        assertThat(res, is(1));
    }

    @Test
    public void testFindDeletedBundleById() {
        given(runtimeMapper.findDeleted(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return RuntimeEntity.builder().id(id).build();
                });

        var res = manager.findDeletedBundleById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findDeletedBundleById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testRecover() {
        given(runtimeMapper.recover(same(1L)))
                .willReturn(1);

        var res = manager.recover(1L);
        assertThat(res, is(true));

        res = manager.recover(2L);
        assertThat(res, is(false));
    }

    @Test
    public void testRemove() {
        given(runtimeMapper.remove(same(1L)))
                .willReturn(1);

        var res = manager.remove(1L);
        assertThat(res, is(true));

        res = manager.remove(2L);
        assertThat(res, is(false));
    }
}
