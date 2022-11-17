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
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.tag.HasTagWrapper;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelManagerTest {

    private ModelManager manager;
    private ModelMapper modelMapper;
    private ModelVersionMapper versionMapper;

    @BeforeEach
    public void setUp() {
        modelMapper = mock(ModelMapper.class);
        versionMapper = mock(ModelVersionMapper.class);
        manager = new ModelManager(
                modelMapper,
                versionMapper,
                new IdConvertor(),
                new VersionAliasConvertor()
        );
    }

    @Test
    public void testFindById() {
        given(modelMapper.findModelById(anyLong()))
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
    public void testFindObjectWithTagById() {
        given(versionMapper.findVersionById(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return ModelVersionEntity.builder().id(id).versionTag("tag").build();
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
        given(modelMapper.findByName(same("m1"), same(1L)))
                .willReturn(ModelEntity.builder().id(1L).build());

        var res = manager.findByName("m1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findByName("m2", 1L);
        assertThat(res, nullValue());
    }

    @Test
    public void testFindVersionById() {
        given(versionMapper.findVersionById(anyLong()))
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
        given(versionMapper.findByVersionOrderAndModelId(anyLong(), anyLong()))
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
        given(versionMapper.getLatestVersion(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).build());

        var res = manager.findLatestVersionByBundleId(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findLatestVersionByBundleId(2L);
        assertThat(res, nullValue());
    }

    @Test
    public void testRevertTo() {
        given(versionMapper.revertTo(same(1L), same(2L)))
                .willReturn(1);

        var res = manager.revertTo(1L, 2L);
        assertThat(res, is(true));

        res = manager.revertTo(2L, 2L);
        assertThat(res, is(false));
    }

    @Test
    public void testFindDeletedBundleById() {
        given(modelMapper.findDeletedModelById(anyLong()))
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
    public void testListDeletedBundlesByName() {
        given(modelMapper.listDeletedModel(same("m1"), same(1L)))
                .willReturn(List.of(ModelEntity.builder().build()));
        given(modelMapper.listDeletedModel(same("m2"), same(1L)))
                .willReturn(List.of());

        var res = manager.listDeletedBundlesByName("m1", 1L);
        assertThat(res, allOf(
                notNullValue(),
                iterableWithSize(1)
        ));

        res = manager.listDeletedBundlesByName("m2", 1L);
        assertThat(res, allOf(
                notNullValue(),
                emptyIterable()
        ));
    }

    @Test
    public void testRecover() {
        given(modelMapper.recoverModel(same(1L)))
                .willReturn(1);

        var res = manager.recover(1L);
        assertThat(res, is(true));

        res = manager.recover(2L);
        assertThat(res, is(false));
    }

    @Test
    public void testRemove() {
        given(modelMapper.deleteModel(same(1L)))
                .willReturn(1);

        var res = manager.remove(1L);
        assertThat(res, is(true));

        res = manager.remove(2L);
        assertThat(res, is(false));
    }
}
