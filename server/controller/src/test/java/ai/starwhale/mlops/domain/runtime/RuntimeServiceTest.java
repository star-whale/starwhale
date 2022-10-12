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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.Mockito.mockStatic;

import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class RuntimeServiceTest {

    private RuntimeService service;
    private RuntimeMapper runtimeMapper;
    private RuntimeVersionMapper runtimeVersionMapper;
    private StorageService storageService;
    private ProjectManager projectManager;
    private RuntimeConvertor runtimeConvertor;
    private RuntimeVersionConvertor versionConvertor;
    private RuntimeManager runtimeManager;
    private StoragePathCoordinator storagePathCoordinator;
    private StorageAccessService storageAccessService;
    private UserService userService;
    private HotJobHolder jobHolder;
    private ObjectMapper yamlMapper;
    @Setter
    private BundleManager bundleManager;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        runtimeMapper = mock(RuntimeMapper.class);
        runtimeVersionMapper = mock(RuntimeVersionMapper.class);
        runtimeConvertor = mock(RuntimeConvertor.class);
        given(runtimeConvertor.convert(any(RuntimeEntity.class)))
                .willAnswer(invocation -> {
                    RuntimeEntity entity = invocation.getArgument(0);
                    return RuntimeVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(RuntimeVersionConvertor.class);
        given(versionConvertor.convert(any(RuntimeVersionEntity.class)))
                .willAnswer(invocation -> {
                    RuntimeVersionEntity entity = invocation.getArgument(0);
                    return RuntimeVersionVo.builder()
                            .id(String.valueOf(entity.getId()))
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
                .willReturn(User.builder().id(1L).idTableKey(1L).build());
        projectManager = mock(ProjectManager.class);
        given(projectManager.getProjectId(same("1")))
                .willReturn(1L);
        given(projectManager.getProjectId(same("2")))
                .willReturn(2L);
        runtimeManager = mock(RuntimeManager.class);
        jobHolder = mock(HotJobHolder.class);

        yamlMapper = new ObjectMapper(new YAMLFactory());

        service = new RuntimeService(
                runtimeMapper,
                runtimeVersionMapper,
                storageService,
                projectManager,
                yamlMapper,
                runtimeConvertor,
                versionConvertor,
                runtimeManager,
                storagePathCoordinator,
                storageAccessService,
                jobHolder,
                userService,
                new IdConvertor(),
                new VersionAliasConvertor()
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
    public void testListRuntime() {
        given(runtimeMapper.listRuntimes(same(1L), anyString()))
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
    public void testRecoverRuntime() {
        RecoverManager recoverManager = mock(RecoverManager.class);
        given(recoverManager.recoverBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "r1")
        ))).willReturn(true);
        try (var mock = mockStatic(RecoverManager.class)) {
            mock.when(() -> RecoverManager.create(any(), any(), any()))
                    .thenReturn(recoverManager);

            var res = service.recoverRuntime("p1", "r1");
            assertThat(res, is(true));

            res = service.recoverRuntime("p1", "r2");
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
        given(runtimeMapper.findByName(same("r1"), same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());
        given(runtimeVersionMapper.listVersions(same(1L), any(), any()))
                .willReturn(List.of(RuntimeVersionEntity.builder().versionOrder(2L).build()));

        var res = service.listRuntimeInfo("1", "r1");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        given(projectManager.findByNameOrDefault(same("1"), same(1L)))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(runtimeMapper.listRuntimes(same(1L), any()))
                .willReturn(List.of(RuntimeEntity.builder().id(1L).build()));

        res = service.listRuntimeInfo("1", "");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        assertThrows(SwValidationException.class,
                () -> service.listRuntimeInfo("2", "r1"));
    }

    @Test
    public void testGetRuntimeInfo() {
        given(runtimeMapper.findRuntimeById(same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());

        given(runtimeMapper.findRuntimeById(same(2L)))
                .willReturn(RuntimeEntity.builder().id(2L).build());

        assertThrows(StarwhaleApiException.class,
                () -> service.getRuntimeInfo(RuntimeQuery.builder().projectUrl("1").runtimeUrl("3").build()));

        given(runtimeVersionMapper.findVersionById(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).versionOrder(2L).build());

        var res = service.getRuntimeInfo(RuntimeQuery.builder()
                .projectUrl("1")
                .runtimeUrl("1")
                .runtimeVersionUrl("1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        given(runtimeVersionMapper.getLatestVersion(same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).versionOrder(2L).build());

        res = service.getRuntimeInfo(RuntimeQuery.builder()
                .projectUrl("1")
                .runtimeUrl("1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        assertThrows(StarwhaleApiException.class,
                () -> service.getRuntimeInfo(RuntimeQuery.builder().projectUrl("1").runtimeUrl("2").build()));
    }

    @Test
    public void testModifyRuntimeVersion() {
        given(runtimeVersionMapper.update(argThat(entity -> entity.getId() == 1L)))
                .willReturn(1);

        var res = service.modifyRuntimeVersion("1", "1", "1", RuntimeVersion.builder().build());
        assertThat(res, is(true));

        res = service.modifyRuntimeVersion("1", "1", "2", RuntimeVersion.builder().build());
        assertThat(res, is(false));
    }

    @Test
    public void testListRuntimeVersionHistory() {
        given(runtimeVersionMapper.listVersions(anyLong(), anyString(), anyString()))
                .willReturn(List.of(RuntimeVersionEntity.builder().id(1L).build()));
        var res = service.listRuntimeVersionHistory(
                RuntimeVersionQuery.builder()
                        .projectUrl("1")
                        .runtimeUrl("1")
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
    public void testFindRuntimeByVersionIds() {
        given(runtimeVersionMapper.findVersionsByIds(anyList()))
                .willReturn(List.of(
                        RuntimeVersionEntity.builder().runtimeId(1L).build()
                ));

        given(runtimeMapper.findRuntimeById(same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());

        var res = service.findRuntimeByVersionIds(List.of());
        assertThat(res, allOf(
                iterableWithSize(1),
                hasItem(hasProperty("id", is("1")))
        ));
    }

    @Test
    public void testUpload() {
        given(projectManager.getProject(anyString()))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(runtimeMapper.findByNameForUpdate(anyString(), same(1L)))
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
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(runtimeMapper.findByName(same("r1"), same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());
        assertThrows(SwValidationException.class,
                () -> service.pull("2", "r2", "v2", response));

        given(runtimeVersionMapper.findByNameAndRuntimeId(same("v1"), same(1L)))
                .willReturn(RuntimeVersionEntity.builder().storagePath("path1").build());
        assertThrows(SwValidationException.class,
                () -> service.pull("1", "r1", "v4", response));

        given(runtimeVersionMapper.findByNameAndRuntimeId(same("v2"), same(1L)))
                .willReturn(RuntimeVersionEntity.builder().storagePath("path2").build());

        given(runtimeVersionMapper.findByNameAndRuntimeId(same("v3"), same(1L)))
                .willReturn(RuntimeVersionEntity.builder().storagePath("path3").build());

        given(storageAccessService.list(anyString())).willThrow(IOException.class);
        given(storageAccessService.list(same("path1"))).willReturn(Stream.of("path1/file1"));
        given(storageAccessService.list(same("path2"))).willReturn(Stream.of());
        assertThrows(SwValidationException.class,
                () -> service.pull("1", "r1", "v2", response));
        assertThrows(SwProcessException.class,
                () -> service.pull("1", "r1", "v3", response));

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
        given(runtimeMapper.findByName(same("r1"), same(1L)))
                .willReturn(RuntimeEntity.builder().id(1L).build());

        given(runtimeVersionMapper.findByNameAndRuntimeId(same("v1"), same(1L)))
                .willReturn(RuntimeVersionEntity.builder().id(1L).versionName("").build());

        var res = service.query("1", "r1", "v1");
        assertThat(res, is(""));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "r2", "v1"));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "r1", "v2"));

    }

}
