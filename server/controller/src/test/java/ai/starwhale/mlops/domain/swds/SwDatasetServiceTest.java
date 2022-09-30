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

package ai.starwhale.mlops.domain.swds;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.Mockito.mockStatic;

import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.swds.DatasetVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swds.bo.SwdsQuery;
import ai.starwhale.mlops.domain.swds.bo.SwdsVersion;
import ai.starwhale.mlops.domain.swds.bo.SwdsVersionQuery;
import ai.starwhale.mlops.domain.swds.converter.SwdsVersionConvertor;
import ai.starwhale.mlops.domain.swds.converter.SwdsVoConvertor;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.objectstore.DsFileGetter;
import ai.starwhale.mlops.domain.swds.po.SwDatasetEntity;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SwDatasetServiceTest {

    private SwDatasetService service;
    private SwDatasetMapper swdsMapper;
    private SwDatasetVersionMapper swdsVersionMapper;
    private SwdsVoConvertor swdsConvertor;
    private SwdsVersionConvertor versionConvertor;
    private StorageService storageService;
    private ProjectManager projectManager;
    private SwdsManager swdsManager;
    private UserService userService;
    private DsFileGetter dsFileGetter;
    @Setter
    private BundleManager bundleManager;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        swdsMapper = mock(SwDatasetMapper.class);
        swdsVersionMapper = mock(SwDatasetVersionMapper.class);
        swdsConvertor = mock(SwdsVoConvertor.class);
        given(swdsConvertor.convert(any(SwDatasetEntity.class)))
                .willAnswer(invocation -> {
                    SwDatasetEntity entity = invocation.getArgument(0);
                    return DatasetVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(SwdsVersionConvertor.class);
        given(versionConvertor.convert(any(SwDatasetVersionEntity.class)))
                .willAnswer(invocation -> {
                    SwDatasetVersionEntity entity = invocation.getArgument(0);
                    return DatasetVersionVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });

        storageService = mock(StorageService.class);
        given(storageService.listStorageFile(any()))
                .willReturn(List.of());
        given(storageService.getStorageSize(any()))
                .willReturn(1000L);

        userService = mock(UserService.class);
        given(userService.currentUserDetail())
                .willReturn(User.builder().id(1L).idTableKey(1L).build());
        projectManager = mock(ProjectManager.class);
        given(projectManager.getProjectId(same("1")))
                .willReturn(1L);
        given(projectManager.getProjectId(same("2")))
                .willReturn(2L);
        swdsManager = mock(SwdsManager.class);

        dsFileGetter = mock(DsFileGetter.class);

        service = new SwDatasetService(
                projectManager,
                swdsMapper,
                swdsVersionMapper,
                swdsConvertor,
                versionConvertor,
                storageService,
                swdsManager,
                new IdConvertor(),
                new VersionAliasConvertor(),
                userService,
                dsFileGetter
        );
        bundleManager = mock(BundleManager.class);
        given(bundleManager.getBundleId(any(BundleUrl.class)))
                .willAnswer(invocation -> {
                    BundleUrl bundleUrl = invocation.getArgument(0);
                    return Long.valueOf(bundleUrl.getBundleUrl());
                });
        given(bundleManager.getBundleVersionId(any(BundleVersionUrl.class)))
                .willAnswer(invocation -> {
                    BundleVersionUrl url = invocation.getArgument(0);
                    return Long.valueOf(url.getVersionUrl());
                });

        given(bundleManager.getBundleVersionId(any(BundleVersionUrl.class), anyLong()))
                .willAnswer(invocation -> {
                    BundleVersionUrl url = invocation.getArgument(0);
                    return Long.valueOf(url.getVersionUrl());
                });

        service.setBundleManager(bundleManager);
    }

    @Test
    public void testListSwmp() {
        given(swdsMapper.listDatasets(same(1L), anyString()))
                .willReturn(List.of(
                        SwDatasetEntity.builder().id(1L).build(),
                        SwDatasetEntity.builder().id(2L).build()
                ));
        var res = service.listSwDataset(SwdsQuery.builder()
                .projectUrl("1")
                .namePrefix("")
                .build(), new PageParams(1, 5));
        assertThat(res, allOf(
                hasProperty("size", is(2)),
                hasProperty("list", hasItem(hasProperty("id", is("1")))),
                hasProperty("list", hasItem(hasProperty("id", is("2"))))
        ));
    }

    @Test
    public void testDeleteSwds() {
        RemoveManager removeManager = mock(RemoveManager.class);
        given(removeManager.removeBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "d1")
        ))).willReturn(true);
        try (var mock = mockStatic(RemoveManager.class)) {
            mock.when(() -> RemoveManager.create(any(), any()))
                    .thenReturn(removeManager);
            var res = service.deleteSwds(SwdsQuery.builder().projectUrl("p1").swdsUrl("d1").build());
            assertThat(res, is(true));

            res = service.deleteSwds(SwdsQuery.builder().projectUrl("p2").swdsUrl("d2").build());
            assertThat(res, is(false));
        }
    }

    @Test
    public void testRecoverSwds() {
        RecoverManager recoverManager = mock(RecoverManager.class);
        given(recoverManager.recoverBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "d1")
        ))).willReturn(true);
        try (var mock = mockStatic(RecoverManager.class)) {
            mock.when(() -> RecoverManager.create(any(), any(), any()))
                    .thenReturn(recoverManager);

            var res = service.recoverSwds("p1", "d1");
            assertThat(res, is(true));

            res = service.recoverSwds("p1", "d2");
            assertThat(res, is(false));
        }
    }

    @Test
    public void testGetSwmpInfo() {
        given(swdsMapper.findDatasetById(same(1L)))
                .willReturn(SwDatasetEntity.builder().id(1L).build());

        given(swdsMapper.findDatasetById(same(2L)))
                .willReturn(SwDatasetEntity.builder().id(2L).build());

        assertThrows(StarwhaleApiException.class,
                () -> service.getSwdsInfo(SwdsQuery.builder().projectUrl("1").swdsUrl("3").build()));

        given(swdsVersionMapper.getVersionById(same(1L)))
                .willReturn(SwDatasetVersionEntity.builder().id(1L).versionOrder(2L).build());

        var res = service.getSwdsInfo(SwdsQuery.builder()
                .projectUrl("1")
                .swdsUrl("1")
                .swdsVersionUrl("1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        given(swdsVersionMapper.getLatestVersion(same(1L)))
                .willReturn(SwDatasetVersionEntity.builder().id(1L).versionOrder(2L).build());

        res = service.getSwdsInfo(SwdsQuery.builder()
                .projectUrl("1")
                .swdsUrl("1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        assertThrows(StarwhaleApiException.class,
                () -> service.getSwdsInfo(SwdsQuery.builder().projectUrl("1").swdsUrl("2").build()));
    }

    @Test
    public void testModifySwdsVersion() {
        given(swdsVersionMapper.update(argThat(entity -> entity.getId() == 1L)))
                .willReturn(1);

        var res = service.modifySwdsVersion("1", "1", "1", new SwdsVersion());
        assertThat(res, is(true));

        res = service.modifySwdsVersion("1", "1", "2", new SwdsVersion());
        assertThat(res, is(false));
    }

    @Test
    public void testRevertVersionTo() {
        RevertManager revertManager = mock(RevertManager.class);
        given(revertManager.revertVersionTo(argThat(
                url ->
                        Objects.equals(url.getBundleUrl().getProjectUrl(), "p1")
                                && Objects.equals(url.getBundleUrl().getBundleUrl(), "m1")
                                && Objects.equals(url.getVersionUrl(), "v1")
        ))).willReturn(true);
        try (var mock = mockStatic(RevertManager.class)) {
            mock.when(() -> RevertManager.create(any(), any()))
                    .thenReturn(revertManager);

            var res = service.revertVersionTo("p1", "m1", "v1");
            assertThat(res, is(true));

            res = service.revertVersionTo("p1", "m1", "v2");
            assertThat(res, is(false));
        }
    }

    @Test
    public void testListSwdsVersionHistory() {
        given(swdsVersionMapper.listVersions(anyLong(), anyString(), anyString()))
                .willReturn(List.of(SwDatasetVersionEntity.builder().id(1L).datasetName("d1").build()));
        var res = service.listDatasetVersionHistory(
                SwdsVersionQuery.builder()
                        .projectUrl("1")
                        .swdsUrl("1")
                        .versionName("v1")
                        .versionTag("tag1")
                        .build(),
                PageParams.builder().build()
        );
        assertThat(res, allOf(
                hasProperty("list", iterableWithSize(1))
        ));
    }

    @Test
    public void testFindDatasetByVersionIds() {
        given(swdsVersionMapper.findVersionsByIds(anyList()))
                .willReturn(List.of(
                        SwDatasetVersionEntity.builder().datasetId(1L).build()
                ));

        given(swdsMapper.findDatasetById(same(1L)))
                .willReturn(SwDatasetEntity.builder().id(1L).build());

        given(swdsMapper.findDatasetsByIds(anyList()))
                .willAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> SwDatasetEntity.builder().id(id).build())
                            .collect(Collectors.toList());
                });

        var res = service.findDatasetsByVersionIds(List.of());
        assertThat(res, allOf(
                iterableWithSize(1),
                hasItem(hasProperty("id", is("1")))
        ));
    }

    @Test
    public void testListSwmpInfo() {
        given(swdsMapper.findByName(same("d1"), same(1L)))
                .willReturn(SwDatasetEntity.builder().id(1L).build());
        given(swdsVersionMapper.listVersions(same(1L), any(), any()))
                .willReturn(List.of(SwDatasetVersionEntity.builder().versionOrder(2L).build()));

        var res = service.listDs("1", "d1");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        given(projectManager.findByNameOrDefault(same("1"), same(1L)))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(swdsMapper.listDatasets(same(1L), any()))
                .willReturn(List.of(SwDatasetEntity.builder().id(1L).build()));

        res = service.listDs("1", "");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        assertThrows(SwValidationException.class,
                () -> service.listDs("2", "d1"));
    }

    @Test
    public void testQuery() {
        given(swdsMapper.findByName(same("d1"), same(1L)))
                .willReturn(SwDatasetEntity.builder().id(1L).build());

        given(swdsVersionMapper.findByDsIdAndVersionName(same(1L), same("v1")))
                .willReturn(SwDatasetVersionEntity.builder().id(1L).build());

        var res = service.query("1", "d1", "v1");
        assertThat(res, hasProperty("id", is(1L)));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "d2", "v1"));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "d1", "v2"));

    }

    @Test
    public void testDataOf() {
        given(dsFileGetter.dataOf(same(1L), anyString(), anyString(), anyString(), anyString()))
                .willReturn(new byte[1]);

        var res = dsFileGetter.dataOf(1L, "", "", "", "");
        assertThat(res, notNullValue());
    }


}
