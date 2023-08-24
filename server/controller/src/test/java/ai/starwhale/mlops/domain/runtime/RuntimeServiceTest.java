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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.domain.bundle.BundleException;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.RunEnvs;
import ai.starwhale.mlops.domain.job.step.VirtualJobLoader;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeConverter;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeVersionConverter;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionViewEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class RuntimeServiceTest {

    private RuntimeService service;
    private RuntimeMapper runtimeMapper;
    private RuntimeVersionMapper runtimeVersionMapper;
    private StorageService storageService;
    private ProjectService projectService;
    private RuntimeConverter runtimeConvertor;
    private RuntimeVersionConverter versionConvertor;
    private RuntimeDao runtimeDao;
    private StoragePathCoordinator storagePathCoordinator;
    private StorageAccessService storageAccessService;
    private UserService userService;
    private HotJobHolder jobHolder;
    private TrashService trashService;
    private BundleVersionTagDao bundleVersionTagDao;
    @Setter
    private BundleManager bundleManager;

    private JobCreator jobCreator;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        runtimeMapper = mock(RuntimeMapper.class);
        runtimeVersionMapper = mock(RuntimeVersionMapper.class);
        runtimeConvertor = mock(RuntimeConverter.class);
        given(runtimeConvertor.convert(any(RuntimeEntity.class)))
                .willAnswer(invocation -> {
                    RuntimeEntity entity = invocation.getArgument(0);
                    return RuntimeVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(RuntimeVersionConverter.class);
        given(versionConvertor.convert(any(RuntimeVersionEntity.class), any(), any()))
                .willAnswer(invocation -> {
                    RuntimeVersionEntity entity = invocation.getArgument(0);
                    return RuntimeVersionVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .alias("v" + entity.getVersionOrder())
                            .name(entity.getName())
                            .build();
                });
        storagePathCoordinator = mock(StoragePathCoordinator.class);
        storageAccessService = mock(StorageAccessService.class);
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
        runtimeDao = mock(RuntimeDao.class);
        jobHolder = mock(HotJobHolder.class);

        trashService = mock(TrashService.class);
        bundleVersionTagDao = mock(BundleVersionTagDao.class);

        jobCreator = mock(JobCreator.class);
        service = new RuntimeService(
                runtimeMapper,
                runtimeVersionMapper,
                bundleVersionTagDao,
                storageService,
                projectService,
                runtimeConvertor,
                versionConvertor,
                runtimeDao,
                storagePathCoordinator,
                storageAccessService,
                jobHolder,
                userService,
                new IdConverter(),
                new VersionAliasConverter(),
                trashService,
                new DockerSetting("localhost:8083", "localhost:8083", "admin", "admin123", false),
                new RunTimeProperties(
                        "",
                        new RunTimeProperties.RunConfig("rc", "", "", ""),
                        new RunTimeProperties.RunConfig("rc", "", "", ""),
                        new RunTimeProperties.Pypi(
                                "https://pypi.io/simple",
                                "https://edu.io/simple",
                                "pypi.io",
                                1,
                                2
                        ),
                        ""
                ),
                jobCreator, new VirtualJobLoader(null), new JobSpecParser()
        );
        bundleManager = mock(BundleManager.class);
        given(bundleManager.getBundleId(any(BundleUrl.class)))
                .willAnswer(invocation -> {
                    BundleUrl bundleUrl = invocation.getArgument(0);
                    switch (bundleUrl.getBundleUrl()) {
                        case "r1":
                            return 1L;
                        case "r2":
                            return 2L;
                        case "r3":
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
        given(bundleManager.getBundle(any())).willReturn(RuntimeEntity.builder().id(1L).runtimeName("test").build());
        given(bundleManager.getBundleVersion(any(BundleVersionUrl.class)))
                .willAnswer((Answer<BundleVersionEntity>) invocation -> {
                    BundleVersionUrl url = invocation.getArgument(0);
                    switch (url.getVersionUrl()) {
                        case "v1":
                            return RuntimeVersionEntity.builder()
                                    .id(1L)
                                    .versionName("n1")
                                    .storagePath("path1")
                                    .versionMeta(RuntimeTestConstants.MANIFEST_WITHOUT_BUILTIN_IMAGE)
                                    .build();
                        case "v2":
                            return RuntimeVersionEntity.builder()
                                    .id(2L)
                                    .versionName("n2")
                                    .storagePath("path2")
                                    .versionMeta(RuntimeTestConstants.MANIFEST_WITHOUT_BUILTIN_IMAGE)
                                    .builtImage("build-image")
                                    .build();

                        case "v3":
                            return RuntimeVersionEntity.builder()
                                    .id(3L)
                                    .versionName("n3")
                                    .versionMeta(RuntimeTestConstants.MANIFEST_WITHOUT_BUILTIN_IMAGE)
                                    .storagePath("path3")
                                    .build();
                        default:
                            throw new BundleException("");
                    }
                });
        service.setBundleManager(bundleManager);
    }

    @Test
    public void testListRuntime() {
        given(runtimeMapper.list(same(1L), anyString(), any(), any()))
                .willReturn(List.of(
                        RuntimeEntity.builder().id(1L).build(),
                        RuntimeEntity.builder().id(2L).build()
                ));
        var res = service.listRuntime(RuntimeQuery.builder()
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
    public void testFindBo() {
        RuntimeEntity r1 = RuntimeEntity.builder().id(1L).runtimeName("rt1").build();
        RuntimeVersionEntity v1 = RuntimeVersionEntity.builder()
                .id(2L)
                .runtimeId(3L)
                .versionName("rt1")
                .ownerId(1L)
                .versionMeta("test_meta")
                .build();
        given(runtimeDao.getRuntime(same(1L)))
                .willReturn(r1);
        given(runtimeDao.getRuntimeVersion(same("v1")))
                .willReturn(v1);
        given(runtimeDao.findVersionById(same(2L)))
                .willReturn(v1);

        var res = service.findRuntime(1L);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(1L)),
                hasProperty("name", is("rt1"))
        ));

        var res1 = service.findRuntimeVersion(2L);
        assertThat(res1, allOf(
                notNullValue(),
                hasProperty("id", is(v1.getId())),
                hasProperty("runtimeId", is(v1.getRuntimeId())),
                hasProperty("versionName", is(v1.getVersionName()))
        ));

        var res2 = service.findRuntimeVersion("v1");
        assertThat(res2, equalTo(res1));
    }

    @Test
    public void testDeleteRuntime() {
        RemoveManager removeManager = mock(RemoveManager.class);
        given(removeManager.removeBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "r1")
        ))).willReturn(true);
        try (var mock = mockStatic(RemoveManager.class)) {
            mock.when(() -> RemoveManager.create(any(), any()))
                    .thenReturn(removeManager);
            var res = service.deleteRuntime(RuntimeQuery.builder().projectUrl("p1").runtimeUrl("r1").build());
            assertThat(res, is(true));

            res = service.deleteRuntime(RuntimeQuery.builder().projectUrl("p2").runtimeUrl("r2").build());
            assertThat(res, is(false));
        }
    }

    @Test
    public void testRevertVersionTo() {
        RevertManager revertManager = mock(RevertManager.class);
        given(revertManager.revertVersionTo(argThat(
                url ->
                        Objects.equals(url.getBundleUrl().getProjectUrl(), "p1")
                                && Objects.equals(url.getBundleUrl().getBundleUrl(), "r1")
                                && Objects.equals(url.getVersionUrl(), "v1")
        ))).willReturn(true);
        try (var mock = mockStatic(RevertManager.class)) {
            mock.when(() -> RevertManager.create(any(), any()))
                    .thenReturn(revertManager);

            var res = service.revertVersionTo("p1", "r1", "v1");
            assertThat(res, is(true));

            res = service.revertVersionTo("p1", "r1", "v2");
            assertThat(res, is(false));
        }
    }

    @Test
    public void testListRuntimeInfo() {
        given(runtimeMapper.findByName(same("r1"), same(1L), any()))
                .willReturn(RuntimeEntity.builder().id(1L).build());
        given(runtimeVersionMapper.list(same(1L), any()))
                .willReturn(List.of(RuntimeVersionEntity.builder().versionOrder(2L).shared(false).build()));

        var res = service.listRuntimeInfo("1", "r1");
        assertEquals(1, res.size());
        assertEquals("1", res.get(0).getId());
        assertEquals("v2", res.get(0).getVersionInfo().getAlias());

        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(runtimeMapper.list(same(1L), any(), any(), any()))
                .willReturn(List.of(RuntimeEntity.builder().id(1L).build()));

        res = service.listRuntimeInfo("1", "");
        assertEquals(1, res.size());
        assertEquals("1", res.get(0).getId());
        assertEquals("v2", res.get(0).getVersionInfo().getAlias());

        assertThrows(
                SwNotFoundException.class,
                () -> service.listRuntimeInfo("2", "r1")
        );
    }

    @Test
    public void testGetRuntimeInfo() {
        given(runtimeMapper.find(same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());

        given(runtimeMapper.find(same(2L)))
                .willReturn(RuntimeEntity.builder().id(2L).build());

        assertThrows(
                SwNotFoundException.class,
                () -> service.getRuntimeInfo(RuntimeQuery.builder().projectUrl("1").runtimeUrl("r3").build())
        );

        given(runtimeVersionMapper.find(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).versionOrder(2L).shared(false).build());

        given(runtimeVersionMapper.findByLatest(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).versionOrder(2L).shared(false).build());

        var res = service.getRuntimeInfo(RuntimeQuery.builder()
                                                 .projectUrl("p1")
                                                 .runtimeUrl("r1")
                                                 .runtimeVersionUrl("v1")
                                                 .build());

        assertEquals("1", res.getId());
        assertEquals("v2", res.getVersionInfo().getAlias());

        given(runtimeVersionMapper.findByLatest(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).versionOrder(2L).shared(false).build());

        res = service.getRuntimeInfo(RuntimeQuery.builder()
                                             .projectUrl("p1")
                                             .runtimeUrl("r1")
                                             .build());

        assertEquals("1", res.getId());
        assertEquals("v2", res.getVersionInfo().getAlias());

        assertThrows(
                BundleException.class,
                () -> service.getRuntimeInfo(RuntimeQuery.builder().projectUrl("1").runtimeUrl("2").build())
        );
    }

    @Test
    public void testModifyRuntimeVersion() {
        given(runtimeVersionMapper.update(argThat(entity -> entity.getId() == 1L)))
                .willReturn(1);

        var res = service.modifyRuntimeVersion("1", "1", "v1", RuntimeVersion.builder().build());
        assertThat(res, is(true));

        res = service.modifyRuntimeVersion("1", "1", "v2", RuntimeVersion.builder().build());
        assertThat(res, is(false));
    }

    @Test
    public void testShareRuntimeVersion() {
        var projectService = mock(ProjectService.class);
        var runtimeDao = mock(RuntimeDao.class);
        var versionAliasConverter = mock(VersionAliasConverter.class);
        var runtimeVersionMapper = mock(RuntimeVersionMapper.class);

        var svc = new RuntimeService(
                mock(RuntimeMapper.class),
                runtimeVersionMapper,
                mock(BundleVersionTagDao.class),
                mock(StorageService.class),
                projectService,
                mock(RuntimeConverter.class),
                mock(RuntimeVersionConverter.class),
                runtimeDao,
                mock(StoragePathCoordinator.class),
                mock(StorageAccessService.class),
                mock(HotJobHolder.class),
                mock(UserService.class),
                new IdConverter(),
                versionAliasConverter,
                mock(TrashService.class),
                mock(DockerSetting.class),
                mock(RunTimeProperties.class),
                mock(JobCreator.class), mock(VirtualJobLoader.class), mock(JobSpecParser.class)
        );

        // public project
        when(projectService.getProjectVo("pub")).thenReturn(ProjectVo.builder().id("1").privacy("PUBLIC").build());
        when(runtimeDao.findById(1L)).thenReturn(RuntimeEntity.builder().id(1L).build());
        when(versionAliasConverter.isVersionAlias("v1")).thenReturn(true);
        var rt = RuntimeVersionEntity.builder().id(2L).build();
        when(runtimeDao.findVersionByAliasAndBundleId("v1", 1L)).thenReturn(rt);
        svc.shareRuntimeVersion("pub", "1", "v1", true);
        verify(runtimeVersionMapper).updateShared(2L, true);
        svc.shareRuntimeVersion("pub", "1", "v1", false);
        verify(runtimeVersionMapper).updateShared(2L, false);

        reset(runtimeVersionMapper);
        // private project can not share resources
        when(projectService.getProjectVo("private")).thenReturn(ProjectVo.builder().id("2").privacy("PRIVATE").build());
        assertThrows(SwValidationException.class, () -> svc.shareRuntimeVersion("private", "1", "v1", true));
        assertThrows(SwValidationException.class, () -> svc.shareRuntimeVersion("private", "1", "v1", false));
        verify(runtimeVersionMapper, never()).updateShared(any(), any());
    }

    @Test
    public void testListRuntimeVersionHistory() {
        given(runtimeVersionMapper.list(anyLong(), anyString()))
                .willReturn(List.of(RuntimeVersionEntity.builder().id(1L).build()));
        var res = service.listRuntimeVersionHistory(
                RuntimeVersionQuery.builder()
                        .projectUrl("1")
                        .runtimeUrl("r1")
                        .versionName("v1")
                        .build(),
                PageParams.builder().build()
        );
        assertThat(res, allOf(
                hasProperty("list", iterableWithSize(1))
        ));
    }

    @Test
    public void testFindRuntimeByVersionIds() {
        given(runtimeVersionMapper.findByIds(anyString()))
                .willReturn(List.of(
                        RuntimeVersionEntity.builder().runtimeId(1L).build()
                ));

        given(runtimeMapper.find(same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());

        var res = service.findRuntimeByVersionIds(List.of(1L));
        assertThat(res, allOf(
                iterableWithSize(1),
                hasItem(hasProperty("id", is("1")))
        ));
    }

    @Test
    public void testUpload() {
        given(projectService.findProject(anyString()))
                .willReturn(Project.builder().id(1L).build());
        given(runtimeMapper.findByName(anyString(), same(1L), any()))
                .willReturn(RuntimeEntity.builder().id(1L).build());
        given(runtimeVersionMapper.findByNameAndRuntimeId(anyString(), same(1L)))
                .willReturn(RuntimeVersionEntity.builder()
                                    .id(1L)
                                    .storagePath("path1")
                                    .build());
        given(jobHolder.ofStatus(anySet()))
                .willReturn(List.of(
                        Job.builder().jobRuntime(JobRuntime.builder().name("r1").version("v1").build()).build(),
                        Job.builder().jobRuntime(JobRuntime.builder().name("r2").version("v2").build()).build()
                ));
        given(storagePathCoordinator.allocateRuntimePath(any(), any(), any()))
                .willReturn("path2");

        try (var mock = mockStatic(TarFileUtil.class)) {
            mock.when(() -> TarFileUtil.getContentFromTarFile(any(), any(), any()))
                    .thenReturn(new byte[]{1});

            ClientRuntimeRequest request = new ClientRuntimeRequest();
            request.setProject("1");
            request.setRuntime("r1:v1");

            MultipartFile dsFile = new MockMultipartFile("dsFile", new byte[10]);
            assertThrows(StarwhaleApiException.class, () -> service.upload(dsFile, request));

            request.setForce("1");
            assertThrows(StarwhaleApiException.class, () -> service.upload(dsFile, request));

            request.setRuntime("r3:v3");
            service.upload(dsFile, request);

            request.setProject("2");
            service.upload(dsFile, request);
        }
    }

    @Test
    public void testPull() throws IOException {
        given(runtimeVersionMapper.find(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().storagePath("path1").build());
        given(runtimeVersionMapper.find(same(2L)))
                .willReturn(RuntimeVersionEntity.builder().storagePath("path2").build());
        given(runtimeVersionMapper.find(same(3L)))
                .willReturn(RuntimeVersionEntity.builder().storagePath("path3").build());

        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(
                BundleException.class,
                () -> service.pull("1", "r1", "v4", response)
        );

        given(storageAccessService.list(anyString())).willThrow(IOException.class);
        given(storageAccessService.list(same("path1"))).willReturn(Stream.of("path1/file1"));
        given(storageAccessService.list(same("path2"))).willReturn(Stream.of());
        assertThrows(
                SwValidationException.class,
                () -> service.pull("1", "r1", "v2", response)
        );
        assertThrows(
                SwProcessException.class,
                () -> service.pull("1", "r1", "v3", response)
        );

        try (LengthAbleInputStream fileInputStream = mock(LengthAbleInputStream.class);
                ServletOutputStream outputStream = mock(ServletOutputStream.class)) {
            given(storageAccessService.get(anyString())).willReturn(fileInputStream);
            given(fileInputStream.transferTo(any())).willReturn(1000L);
            given(response.getOutputStream()).willReturn(outputStream);

            service.pull("1", "r1", "v1", response);
        }

    }

    @Test
    public void testQuery() {
        var res = service.query("1", "r1", "v1");
        assertThat(res, is("n1"));

        res = service.query("p1", "r2", "v2");
        assertThat(res, is("n2"));

        res = service.query("p1", "r1", "v3");
        assertThat(res, is("n3"));
    }

    @Test
    public void testDockerize() {
        Project project = Project.builder().id(1L).name("starwhale").build();
        given(projectService.findProject("project-1"))
                .willReturn(project);
        User user = User.builder().id(1L).name("sw").build();
        given(userService.currentUserDetail()).willReturn(user);
        when(bundleManager.getBundleVersion(any())).thenReturn(RuntimeVersionEntity.builder()
                                                                       .versionName("v1")
                                                                       .builtImage(null)
                                                                       .build());
        when(bundleManager.getBundle(any())).thenReturn(RuntimeEntity.builder().runtimeName("rt").build());
        when(jobCreator.createJob(
                eq(project),
                eq(null),
                eq(null),
                eq(null),
                any(),
                eq("rc"),
                eq(null),
                any(),
                eq(JobType.BUILT_IN),
                eq(null),
                eq(false),
                eq(null),
                eq(null),
                eq(user)
        )).thenReturn(Job.builder().id(1L).build());
        service.dockerize("project-1", "v1", "v1", new RunEnvs(Map.of("k", "v")));
        verify(
                jobCreator,
                times(1)
        ).createJob(
                eq(project),
                eq(null),
                eq(null),
                eq(null),
                any(),
                eq("rc"),
                eq(null),
                eq("---\n"
                           + "- concurrency: 1\n"
                           + "  env:\n"
                           + "  - name: \"k\"\n"
                           + "    value: \"v\"\n"
                           + "  - name: \"SW_TARGET_IMAGE\"\n"
                           + "    value: \"localhost:8083/rt:v1\"\n"
                           + "  - name: \"SW_DEST_IMAGE\"\n"
                           + "    value: \"localhost:8083/rt:v1\"\n"
                           + "  - name: \"SW_RUNTIME_VERSION\"\n"
                           + "    value: \"rt/version/v1\"\n"
                           + "  replicas: 1\n"
                           + "  job_name: \"runtime_dockerizing\"\n"
                           + "  name: \"runtime_dockerizing\"\n"
                           + "  show_name: \"runtime_dockerizing\"\n"
                           + "  require_dataset: false\n"
                           + "  container_spec:\n"
                           + "    image: \"docker-registry.starwhale.cn/star-whale/runtime-dockerizing:latest\"\n"
                           + "    cmds:\n"
                           + "    - \"oho\"\n"
                           + "    entrypoint:\n"
                           + "    - \"sh\"\n"
                           + "    - \"-c\"\n"),
                eq(JobType.BUILT_IN),
                eq(null),
                eq(false),
                eq(null),
                eq(null),
                eq(user)
        );


    }

    @Test
    public void testDockerizeCheck() {
        when(bundleManager.getBundleVersion(any())).thenReturn(null);
        Assertions.assertThrows(SwNotFoundException.class, () -> service.dockerize("project-1", "rt", "v1", null));
    }

    @Test
    public void testDockerizeNotFound() {
        when(bundleManager.getBundleVersion(any())).thenReturn(null);
        Assertions.assertThrows(SwNotFoundException.class, () -> service.dockerize("project-1", "rt", "v1", null));
    }

    @Test
    public void testDockerizeWontDo() {
        when(bundleManager.getBundleVersion(any())).thenReturn(RuntimeVersionEntity.builder().builtImage("x").build());
        service.dockerize("project-1", "rt", "v1", null);
        verify(
                jobCreator,
                times(0)
        ).createJob(
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
                eq(false),
                any(),
                any(),
                any()
        );
    }


    @Test
    public void testListRuntimeVersionView() {
        given(runtimeVersionMapper.findByLatest(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(5L).build());
        given(runtimeVersionMapper.findByLatest(same(3L)))
                .willReturn(RuntimeVersionEntity.builder().id(2L).build());
        given(runtimeVersionMapper.listRuntimeVersionViewByProject(same(1L)))
                .willReturn(List.of(
                        RuntimeVersionViewEntity.builder().id(5L).runtimeId(1L).versionOrder(4L).projectName("sw")
                                .userName("sw").shared(false).runtimeName("rt1").build(),
                        RuntimeVersionViewEntity.builder().id(4L).runtimeId(1L).versionOrder(2L).projectName("sw")
                                .userName("sw").shared(false).runtimeName("rt1").build(),
                        RuntimeVersionViewEntity.builder().id(3L).runtimeId(1L).versionOrder(3L).projectName("sw")
                                .userName("sw").shared(false).runtimeName("rt1").build(),
                        RuntimeVersionViewEntity.builder().id(2L).runtimeId(3L).versionOrder(2L).projectName("sw")
                                .userName("sw").shared(false).runtimeName("rt3").build(),
                        RuntimeVersionViewEntity.builder().id(1L).runtimeId(3L).versionOrder(1L).projectName("sw")
                                .userName("sw").shared(false).runtimeName("rt3").build()
                ));

        given(runtimeVersionMapper.listRuntimeVersionViewByShared(same(1L)))
                .willReturn(List.of(
                        RuntimeVersionViewEntity.builder().id(8L).runtimeId(2L).versionOrder(3L).projectName("sw2")
                                .userName("sw2").shared(true).runtimeName("rt2").build(),
                        RuntimeVersionViewEntity.builder().id(7L).runtimeId(2L).versionOrder(2L).projectName("sw2")
                                .userName("sw2").shared(true).runtimeName("rt2").build(),
                        RuntimeVersionViewEntity.builder().id(6L).runtimeId(4L).versionOrder(3L).projectName("sw2")
                                .userName("sw2").shared(true).runtimeName("rt4").build()
                ));
        given(runtimeVersionMapper.findByLatest(same(2L)))
                .willReturn(RuntimeVersionEntity.builder().id(8L).build());
        given(runtimeVersionMapper.findByLatest(same(4L)))
                .willReturn(RuntimeVersionEntity.builder().id(6L).build());

        var res = service.listRuntimeVersionView("1");
        assertEquals(4, res.size());
        assertEquals("rt1", res.get(0).getRuntimeName());
        assertEquals("rt3", res.get(1).getRuntimeName());
        assertEquals("rt2", res.get(2).getRuntimeName());
        assertEquals("rt4", res.get(3).getRuntimeName());
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
}
