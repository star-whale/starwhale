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

package ai.starwhale.mlops.domain.swmp;

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
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SwmpManagerTest {

    private SwmpManager manager;
    private SwModelPackageMapper modelMapper;
    private SwModelPackageVersionMapper versionMapper;

    @BeforeEach
    public void setUp() {
        modelMapper = mock(SwModelPackageMapper.class);
        versionMapper = mock(SwModelPackageVersionMapper.class);
        manager = new SwmpManager(
                modelMapper,
                versionMapper,
                new IdConvertor(),
                new VersionAliasConvertor()
        );
    }

    @Test
    public void testFindById() {
        given(modelMapper.findSwModelPackageById(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return SwModelPackageEntity.builder().id(id).build();
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
                    return SwModelPackageVersionEntity.builder().id(id).versionTag("tag").build();
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
                .willReturn(SwModelPackageEntity.builder().id(1L).build());

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
                    return SwModelPackageVersionEntity.builder().id(id).build();
                });

        var res = manager.findVersionById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testFindVersionByAliasAndBundleId() {
        given(versionMapper.findByVersionOrderAndSwmpId(anyLong(), anyLong()))
                .willReturn(SwModelPackageVersionEntity.builder()
                        .id(1L)
                        .versionName("model1")
                        .build());
        var res = manager.findVersionByAliasAndBundleId("v1", 1L);
        assertThat(res, allOf(
                notNullValue(),
                isA(SwModelPackageVersionEntity.class),
                hasProperty("id", is(1L)),
                hasProperty("versionName", is("model1"))
        ));
    }

    @Test
    public void testFindVersionByNameAndBundleId() {
        given(versionMapper.findByNameAndSwmpId(same("m1"), same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().id(1L).build());

        var res = manager.findVersionByNameAndBundleId("m1", 1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findVersionByNameAndBundleId("m2", 1L);
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
        given(modelMapper.findDeletedSwModelPackageById(anyLong()))
                .willAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return SwModelPackageEntity.builder().id(id).build();
                });

        var res = manager.findDeletedBundleById(1L);
        assertThat(res, hasProperty("id", is(1L)));

        res = manager.findDeletedBundleById(2L);
        assertThat(res, hasProperty("id", is(2L)));
    }

    @Test
    public void testListDeletedBundlesByName() {
        given(modelMapper.listDeletedSwModelPackages(same("m1"), same(1L)))
                .willReturn(List.of(SwModelPackageEntity.builder().build()));
        given(modelMapper.listDeletedSwModelPackages(same("m2"), same(1L)))
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
        given(modelMapper.recoverSwModelPackage(same(1L)))
                .willReturn(1);

        var res = manager.recover(1L);
        assertThat(res, is(true));

        res = manager.recover(2L);
        assertThat(res, is(false));
    }

    @Test
    public void testRemove() {
        given(modelMapper.deleteSwModelPackage(same(1L)))
                .willReturn(1);

        var res = manager.remove(1L);
        assertThat(res, is(true));

        res = manager.remove(2L);
        assertThat(res, is(false));
    }
}
