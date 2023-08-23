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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.BundleException;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersionQuery;
import ai.starwhale.mlops.domain.dataset.build.BuildType;
import ai.starwhale.mlops.domain.dataset.build.bo.CreateBuildRecordRequest;
import ai.starwhale.mlops.domain.dataset.build.mapper.BuildRecordMapper;
import ai.starwhale.mlops.domain.dataset.build.po.BuildRecordEntity;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVersionVoConverter;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVoConverter;
import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionViewEntity;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.step.VirtualJobLoader;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.storage.UriAccessor;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwValidationException;
import io.kubernetes.client.openapi.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import lombok.SneakyThrows;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class DatasetServiceTest {

    private DatasetService service;
    private DatasetMapper datasetMapper;
    private DatasetVersionMapper datasetVersionMapper;
    private BuildRecordMapper buildRecordMapper;
    private DatasetVoConverter datasetConvertor;
    private DatasetVersionVoConverter versionConvertor;
    private StorageService storageService;
    private ProjectService projectService;
    private DatasetDao datasetDao;
    private UserService userService;
    private UriAccessor uriAccessor;
    private DataLoader dataLoader;
    private TrashService trashService;
    private SystemSettingService systemSettingService;
    private BundleVersionTagDao bundleVersionTagDao;
    @Setter
    private BundleManager bundleManager;

    private JobCreator jobCreator;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        datasetMapper = mock(DatasetMapper.class);
        datasetVersionMapper = mock(DatasetVersionMapper.class);
        buildRecordMapper = mock(BuildRecordMapper.class);
        datasetConvertor = mock(DatasetVoConverter.class);
        given(datasetConvertor.convert(any(DatasetEntity.class)))
                .willAnswer(invocation -> {
                    DatasetEntity entity = invocation.getArgument(0);
                    return DatasetVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(DatasetVersionVoConverter.class);
        given(versionConvertor.convert(any(DatasetVersionEntity.class), any(), any()))
                .willAnswer(invocation -> {
                    DatasetVersionEntity entity = invocation.getArgument(0);
                    return DatasetVersionVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .alias("v" + entity.getVersionOrder())
                            .build();
                });

        storageService = mock(StorageService.class);
        given(storageService.listStorageFile(any()))
                .willReturn(List.of());
        given(storageService.getStorageSize(any()))
                .willReturn(1000L);

        userService = mock(UserService.class);
        given(userService.currentUserDetail())
                .willReturn(User.builder().id(1L).build());
        projectService = mock(ProjectService.class);
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(projectService.getProjectId(same("2")))
                .willReturn(2L);
        datasetDao = mock(DatasetDao.class);

        uriAccessor = mock(UriAccessor.class);

        dataLoader = mock(DataLoader.class);

        trashService = mock(TrashService.class);
        systemSettingService = new SystemSettingService(null, List.of(),
                new RunTimeProperties(
                        "",
                        new RunTimeProperties.RunConfig(),
                        new RunTimeProperties.RunConfig(),
                        new RunTimeProperties.Pypi(
                                "url1",
                                "url2",
                                "host1",
                                11,
                                91
                        ),
                        ""
                ),
                new DockerSetting("", "", "", "", false), userService
        );
        bundleVersionTagDao = mock(BundleVersionTagDao.class);

        jobCreator = mock(JobCreator.class);
        when(jobCreator.createJob(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any()
        )).thenReturn(new JobMockHolder().mockJob());
        service = new DatasetService(
                projectService,
                datasetMapper,
                datasetVersionMapper,
                bundleVersionTagDao,
                buildRecordMapper,
                datasetConvertor,
                versionConvertor,
                storageService,
                datasetDao,
                new IdConverter(),
                new VersionAliasConverter(),
                userService,
                uriAccessor,
                dataLoader,
                trashService,
                systemSettingService, jobCreator, new VirtualJobLoader(null),
                new JobSpecParser(), mock(TaskMapper.class));
        bundleManager = mock(BundleManager.class);
        given(bundleManager.getBundleId(any(BundleUrl.class)))
                .willAnswer(invocation -> {
                    BundleUrl bundleUrl = invocation.getArgument(0);
                    switch (bundleUrl.getBundleUrl()) {
                        case "d1":
                            return 1L;
                        case "d2":
                            return 2L;
                        case "d3":
                            return 3L;
                        default:
                            throw new BundleException("");
                    }
                });

        given(bundleManager.getBundleVersionId(any(BundleVersionUrl.class)))
                .willAnswer((Answer<Long>) invocation -> {
                    BundleVersionUrl url = invocation.getArgument(0);
                    switch (url.getVersionUrl()) {
                        case "v1":
                            return 1L;
                        case "v2":
                            return 2L;
                        case "v3":
                            return 3L;
                        default:
                            throw new BundleException("");
                    }
                });

        service.setBundleManager(bundleManager);
    }

    @Test
    public void testList() {
        given(datasetMapper.list(same(1L), anyString(), any(), any()))
                .willReturn(List.of(
                        DatasetEntity.builder().id(1L).build(),
                        DatasetEntity.builder().id(2L).build()
                ));
        var res = service.listDataset(DatasetQuery.builder()
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
    public void testDeleteDataset() {
        RemoveManager removeManager = mock(RemoveManager.class);
        given(removeManager.removeBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "d1")
        ))).willReturn(true);
        try (var mock = mockStatic(RemoveManager.class)) {
            mock.when(() -> RemoveManager.create(any(), any()))
                    .thenReturn(removeManager);
            var res = service.deleteDataset(DatasetQuery.builder().projectUrl("p1").datasetUrl("d1").build());
            assertThat(res, is(true));

            res = service.deleteDataset(DatasetQuery.builder().projectUrl("p2").datasetUrl("d2").build());
            assertThat(res, is(false));
        }
    }

    @Test
    public void testGetDatasetInfo() {
        given(datasetMapper.find(same(1L)))
                .willReturn(DatasetEntity.builder().id(1L).build());

        given(datasetMapper.find(same(2L)))
                .willReturn(DatasetEntity.builder().id(2L).build());

        assertThrows(
                SwNotFoundException.class,
                () -> service.getDatasetInfo(DatasetQuery.builder().projectUrl("1").datasetUrl("d3").build())
        );

        given(datasetVersionMapper.find(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).versionOrder(2L).shared(false).build());

        given(datasetVersionMapper.findByLatest(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).versionOrder(2L).shared(false).build());

        var res = service.getDatasetInfo(
                DatasetQuery.builder()
                        .projectUrl("1")
                        .datasetUrl("d1")
                        .datasetVersionUrl("v1")
                        .build());

        assertEquals("1", res.getId());
        assertEquals("v2", res.getVersionInfo().getAlias());

        given(datasetVersionMapper.findByLatest(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).versionOrder(2L).shared(false).build());

        res = service.getDatasetInfo(
                DatasetQuery.builder()
                        .projectUrl("1")
                        .datasetUrl("d1")
                        .build());
        assertEquals("1", res.getId());
        assertEquals("v2", res.getVersionInfo().getAlias());

        assertThrows(
                SwNotFoundException.class,
                () -> service.getDatasetInfo(DatasetQuery.builder().projectUrl("1").datasetUrl("d2").build())
        );
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
    public void testListDatasetVersionHistory() {
        given(datasetVersionMapper.list(anyLong(), anyString()))
                .willReturn(List.of(DatasetVersionEntity.builder().id(1L).build()));
        var res = service.listDatasetVersionHistory(
                DatasetVersionQuery.builder()
                        .projectUrl("1")
                        .datasetUrl("d1")
                        .versionName("v1")
                        .build(),
                PageParams.builder().build()
        );
        assertThat(res, allOf(
                hasProperty("list", iterableWithSize(1))
        ));
    }

    @Test
    public void testFindDatasetByVersionIds() {
        given(datasetVersionMapper.findByIds(anyString()))
                .willReturn(List.of(
                        DatasetVersionEntity.builder().datasetId(1L).build()
                ));

        given(datasetMapper.find(same(1L)))
                .willReturn(DatasetEntity.builder().id(1L).build());

        var res = service.findDatasetsByVersionIds(List.of(1L));
        assertThat(res, allOf(
                iterableWithSize(1),
                hasItem(hasProperty("id", is("1")))
        ));
    }

    @Test
    public void testListDatasetInfo() {
        given(datasetMapper.findByName(same("d1"), same(1L), any()))
                .willReturn(DatasetEntity.builder().id(1L).build());
        given(datasetVersionMapper.list(same(1L), any()))
                .willReturn(List.of(DatasetVersionEntity.builder().versionOrder(2L).shared(false).build()));

        var res = service.listDs("1", "d1");
        assertEquals(1, res.size());
        assertEquals("1", res.get(0).getId());
        assertEquals("v2", res.get(0).getVersionInfo().getAlias());

        given(projectService.findProject(same("1")))
                .willReturn(Project.builder().id(1L).build());
        given(datasetMapper.list(same(1L), any(), any(), any()))
                .willReturn(List.of(DatasetEntity.builder().id(1L).build()));

        res = service.listDs("1", "");
        assertEquals(1, res.size());
        assertEquals("1", res.get(0).getId());
        assertEquals("v2", res.get(0).getVersionInfo().getAlias());

        assertThrows(SwNotFoundException.class, () -> service.listDs("2", "d1"));
    }

    @Test
    public void testQuery() {
        given(datasetDao.getDatasetVersion(same(1L)))
                .willReturn(DatasetVersion.builder().id(1L).build());
        var res = service.query("1", "d1", "v1");
        assertThat(res, hasProperty("id", is(1L)));

        res = service.query("1", "d1", "v2");
        assertThat(res, nullValue());

    }

    @Test
    public void testDataOf() {
        given(uriAccessor.dataOf(same(1L), anyString(), anyString(), any(), any()))
                .willReturn(new byte[1]);

        when(projectService.findProject(anyString())).thenReturn(Project.builder().id(1L).build());
        var res = service.dataOf("", "", "", 1L, 1L);
        assertThat(res, notNullValue());
    }

    @Test
    public void testLinkOf() {
        given(uriAccessor.linkOf(same(1L), anyString(), anyString(), anyLong()))
                .willReturn("link");

        when(projectService.findProject(anyString())).thenReturn(Project.builder().id(1L).build());
        assertThat(service.signLink("", "", "", 1L), is("link"));
    }

    @Test
    public void testLinksOf() {
        given(uriAccessor.linkOf(same(1L), anyString(), eq("a"), anyLong()))
                .willReturn("link1");

        given(uriAccessor.linkOf(same(1L), anyString(), eq("b"), anyLong()))
                .willReturn("link2");
        given(uriAccessor.linkOf(same(1L), anyString(), eq("x"), anyLong()))
                .willThrow(SwValidationException.class);

        when(projectService.findProject(anyString())).thenReturn(Project.builder().id(1L).build());
        Assertions.assertEquals(
                Map.of("a", "link1", "b", "link2", "x", ""),
                service.signLinks("", "", Set.of("a", "b", "x"), 1L)
        );
    }

    @Test
    public void testShareDatasetVersion() {
        var projectService = mock(ProjectService.class);
        var datasetDao = mock(DatasetDao.class);
        var versionAliasConverter = mock(VersionAliasConverter.class);
        var datasetVersionMapper = mock(DatasetVersionMapper.class);

        var svc = new DatasetService(
                projectService,
                mock(DatasetMapper.class),
                datasetVersionMapper,
                mock(BundleVersionTagDao.class),
                mock(BuildRecordMapper.class),
                mock(DatasetVoConverter.class),
                mock(DatasetVersionVoConverter.class),
                mock(StorageService.class),
                datasetDao,
                new IdConverter(),
                versionAliasConverter,
                mock(UserService.class),
                mock(UriAccessor.class),
                mock(DataLoader.class),
                mock(TrashService.class),
                mock(SystemSettingService.class), jobCreator, new VirtualJobLoader(null),
                new JobSpecParser(), mock(TaskMapper.class));

        // public project
        when(projectService.getProjectVo("pub")).thenReturn(ProjectVo.builder().id("1").privacy("PUBLIC").build());
        when(datasetDao.findById(1L)).thenReturn(DatasetEntity.builder().id(1L).build());
        when(versionAliasConverter.isVersionAlias("v1")).thenReturn(true);
        var ds = DatasetVersionEntity.builder().id(2L).build();
        when(datasetDao.findVersionByAliasAndBundleId("v1", 1L)).thenReturn(ds);
        svc.shareDatasetVersion("pub", "1", "v1", true);
        verify(datasetVersionMapper).updateShared(2L, true);
        svc.shareDatasetVersion("pub", "1", "v1", false);
        verify(datasetVersionMapper).updateShared(2L, false);

        reset(datasetVersionMapper);
        // private project can not share resources
        when(projectService.getProjectVo("private")).thenReturn(ProjectVo.builder().id("2").privacy("PRIVATE").build());
        assertThrows(SwValidationException.class, () -> svc.shareDatasetVersion("private", "1", "v1", true));
        assertThrows(SwValidationException.class, () -> svc.shareDatasetVersion("private", "1", "v1", false));
        verify(datasetVersionMapper, never()).updateShared(any(), any());
    }

    @Test
    public void testListDatasetVersionView() {
        given(datasetVersionMapper.findByLatest(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(5L).build());
        given(datasetVersionMapper.findByLatest(same(3L)))
                .willReturn(DatasetVersionEntity.builder().id(2L).build());
        given(datasetVersionMapper.listDatasetVersionViewByProject(same(1L)))
                .willReturn(List.of(
                        DatasetVersionViewEntity.builder().id(5L).datasetId(1L).versionOrder(4L).projectName("sw")
                                .userName("sw").shared(false).datasetName("ds1").build(),
                        DatasetVersionViewEntity.builder().id(4L).datasetId(1L).versionOrder(2L).projectName("sw")
                                .userName("sw").shared(false).datasetName("ds1").build(),
                        DatasetVersionViewEntity.builder().id(3L).datasetId(1L).versionOrder(3L).projectName("sw")
                                .userName("sw").shared(false).datasetName("ds1").build(),
                        DatasetVersionViewEntity.builder().id(2L).datasetId(3L).versionOrder(2L).projectName("sw")
                                .userName("sw").shared(false).datasetName("ds3").build(),
                        DatasetVersionViewEntity.builder().id(1L).datasetId(3L).versionOrder(1L).projectName("sw")
                                .userName("sw").shared(false).datasetName("ds3").build()
                ));

        given(datasetVersionMapper.listDatasetVersionViewByShared(same(1L)))
                .willReturn(List.of(
                        DatasetVersionViewEntity.builder().id(8L).datasetId(2L).versionOrder(3L).projectName("sw2")
                                .userName("sw2").shared(true).datasetName("ds2").build(),
                        DatasetVersionViewEntity.builder().id(7L).datasetId(2L).versionOrder(2L).projectName("sw2")
                                .userName("sw2").shared(true).datasetName("ds2").build(),
                        DatasetVersionViewEntity.builder().id(6L).datasetId(4L).versionOrder(3L).projectName("sw2")
                                .userName("sw2").shared(true).datasetName("ds4").build()
                ));
        given(datasetVersionMapper.findByLatest(same(2L)))
                .willReturn(DatasetVersionEntity.builder().id(8L).build());
        given(datasetVersionMapper.findByLatest(same(4L)))
                .willReturn(DatasetVersionEntity.builder().id(6L).build());

        var res = service.listDatasetVersionView("1");
        assertEquals(4, res.size());
        assertEquals("ds1", res.get(0).getDatasetName());
        assertEquals("ds3", res.get(1).getDatasetName());
        assertEquals("ds2", res.get(2).getDatasetName());
        assertEquals("ds4", res.get(3).getDatasetName());
        assertEquals(3, res.get(0).getVersions().size());
        assertEquals(2, res.get(1).getVersions().size());
        assertEquals(2, res.get(2).getVersions().size());
        assertEquals(1, res.get(3).getVersions().size());
        assertEquals("v4", res.get(0).getVersions().get(0).getAlias());
        assertTrue(res.get(0).getVersions().get(0).getLatest());
        assertEquals("v2", res.get(1).getVersions().get(0).getAlias());
        assertTrue(res.get(1).getVersions().get(0).getLatest());
        assertEquals("v3", res.get(2).getVersions().get(0).getAlias());
        assertEquals("v3", res.get(3).getVersions().get(0).getAlias());
    }

    @Test
    public void testStartBuild() throws ApiException {
        var datasetName = "test-build-ds";
        var projectId = 1L;
        given(projectService.findProject(String.valueOf(projectId)))
                .willReturn(Project.builder().id(projectId).build());

        // case1-1: patch and the dataset is not exist
        given(datasetMapper.find(1L)).willReturn(null);
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(1L)
                .datasetName(datasetName)
                .projectUrl(String.valueOf(projectId))
                .build())
        );
        // case1-2: patch and the dataset name in param is not right
        given(datasetMapper.find(1L)).willReturn(DatasetEntity.builder().datasetName("already-dataset").build());
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(1L)
                .datasetName(datasetName)
                .projectUrl(String.valueOf(projectId))
                .build())
        );
        given(datasetMapper.find(1L)).willReturn(DatasetEntity.builder().datasetName("Test-build-ds").build());
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(1L)
                .datasetName(datasetName)
                .projectUrl(String.valueOf(projectId))
                .build())
        );

        // case2: create and already exist the same name dataset
        given(datasetMapper.findByName(datasetName, projectId, true))
                .willReturn(DatasetEntity.builder().build());
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(null)
                .datasetName(datasetName)
                .projectUrl(String.valueOf(projectId))
                .build())
        );

        // case3: create and not exist the same name, but already building a same name dataset
        given(datasetMapper.findByName(datasetName, projectId, true)).willReturn(null);
        given(buildRecordMapper.selectBuildingInOneProjectForUpdate(projectId, datasetName))
                .willReturn(List.of(BuildRecordEntity.builder().build()));
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(null)
                .datasetName(datasetName)
                .projectUrl(String.valueOf(projectId))
                .build())
        );

        // case4: insert to db failed
        given(buildRecordMapper.selectBuildingInOneProjectForUpdate(projectId, datasetName)).willReturn(List.of());
        given(buildRecordMapper.insert(any())).willReturn(0);
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(null)
                .datasetName(datasetName)
                .projectUrl(String.valueOf(projectId))
                .build())
        );

        // case5: normal build
        given(buildRecordMapper.insert(any())).willReturn(1);

        // without configs
        assertThrows(SwValidationException.class, () -> service.build(CreateBuildRecordRequest.builder()
                .datasetId(null)
                .datasetName(datasetName)
                .shared(true)
                .type(BuildType.IMAGE)
                .projectUrl(String.valueOf(projectId))
                .storagePath("storage-path")
                .build()));
        // set config
        systemSettingService.getRunTimeProperties().setDatasetBuild(
                new RunTimeProperties.RunConfig("default", "image", "0.5.6", "3.10"));
        service.build(CreateBuildRecordRequest.builder()
                .datasetId(null)
                .datasetName(datasetName)
                .shared(true)
                .type(BuildType.IMAGE)
                .projectUrl(String.valueOf(projectId))
                .storagePath("storage-path")
                .build());
    }

    @Test
    public void testListBuildRecord() {
        Long project = 1L;
        given(projectService.findProject(String.valueOf(project))).willReturn(Project.builder().id(project).build());
        given(buildRecordMapper.list(project))
                .willReturn(List.of(BuildRecordEntity.builder().id(10L).datasetName("ds").build()));

        var page = service.listBuildRecords(
                String.valueOf(project), PageParams.builder().pageNum(1).pageSize(10).build());
        assertThat(page, allOf(
                hasProperty("list", iterableWithSize(1))
        ));
    }

    @Test
    public void testAddDatasetVersionTag() {
        when(userService.currentUserDetail()).thenReturn(User.builder().id(1L).build());
        doNothing().when(bundleManager).addBundleVersionTag(
                BundleAccessor.Type.DATASET,
                "p1",
                "d1",
                "v1",
                "tag1",
                1L,
                false
        );
        service.addDatasetVersionTag("p1", "d1", "v1", "tag1", false);
        verify(bundleManager, times(1)).addBundleVersionTag(
                BundleAccessor.Type.DATASET,
                "p1",
                "d1",
                "v1",
                "tag1",
                1L,
                false
        );
    }

    @Test
    public void testListDatasetVersionTags() {
        when(bundleManager.listBundleVersionTags(BundleAccessor.Type.DATASET, "p1", "d1", "v1"))
                .thenReturn(List.of("tag1", "tag2"));
        var res = service.listDatasetVersionTags("p1", "d1", "v1");
        AssertionsForInterfaceTypes.assertThat(res).containsExactly("tag1", "tag2");
    }

    @Test
    public void testDeleteDatasetVersionTag() {
        doNothing().when(bundleManager).deleteBundleVersionTag(BundleAccessor.Type.DATASET, "p1", "d1", "v1", "tag1");
        service.deleteDatasetVersionTag("p1", "d1", "v1", "tag1");
        verify(bundleManager, times(1)).deleteBundleVersionTag(BundleAccessor.Type.DATASET, "p1", "d1", "v1", "tag1");
    }
}
