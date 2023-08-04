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

package ai.starwhale.mlops.domain.model;

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
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelDaoTest {

    private ModelDao manager;
    private ModelMapper modelMapper;
    private ModelVersionMapper versionMapper;

    @BeforeEach
    public void setUp() {
        modelMapper = mock(ModelMapper.class);
        versionMapper = mock(ModelVersionMapper.class);
        manager = new ModelDao(
                modelMapper,
                versionMapper,
                new IdConverter(),
                new VersionAliasConverter()
        );
    }

    @Test
    public void testFindById() {
        given(modelMapper.find(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return ModelEntity.builder().id(id).build();
                });
        var res = manager.findById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindByName() {
        given(modelMapper.findByName(same("m1"), same(1L), any()))
                .willReturn(ModelEntity.builder().id(1L).build());

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
                    return ModelVersionEntity.builder().id(id).build();
                });

        var res = manager.findVersionById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindVersionByAliasAndBundleId() {
        given(versionMapper.findByVersionOrder(anyLong(), anyLong()))
                .willReturn(ModelVersionEntity.builder()
                        .id(1L)
                        .versionName("model1")
                        .build());
        var res = manager.findVersionByAliasAndBundleId("v1", 1L);
        assertThat(res, allOf(
                notNullValue(),
                isA(ModelVersionEntity.class),
                hasProperty("id", is(1L)),
                hasProperty("versionName", is("model1"))
        ));
    }

    @Test
    public void testFindVersionByNameAndBundleId() {
        given(versionMapper.findByNameAndModelId(same("m1"), same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).build());

        var res = manager.findVersionByNameAndBundleId("m1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionByNameAndBundleId("m2", 1L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindLatestVersionByBundleId() {
        given(versionMapper.findByLatest(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).build());

        var res = manager.findLatestVersionByBundleId(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findLatestVersionByBundleId(2L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindDeletedBundleById() {
        given(modelMapper.findDeleted(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return ModelEntity.builder().id(id).build();
                });

        var res = manager.findDeletedBundleById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findDeletedBundleById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testRevertTo() {
        Long bundleId = 3L;
        Long versionId = 1L;
        Long maxOrder = 4L;
        given(versionMapper.selectVersionOrderForUpdate(same(versionId)))
                .willReturn(2L);

        given(versionMapper.selectMaxVersionOrderOfModelForUpdate(same(bundleId)))
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
    public void testRecover() {
        given(modelMapper.recover(same(1L)))
                .willReturn(1);

        var res = manager.recover(1L);
        assertThat(res, is(true));

        res = manager.recover(2L);
        assertThat(res, is(false));
    }

    @Test
    public void testRemove() {
        given(modelMapper.remove(same(1L)))
                .willReturn(1);

        var res = manager.remove(1L);
        assertThat(res, is(true));

        res = manager.remove(2L);
        assertThat(res, is(false));
    }


}
