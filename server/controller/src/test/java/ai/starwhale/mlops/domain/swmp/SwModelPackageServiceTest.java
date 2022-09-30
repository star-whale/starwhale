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

import ai.starwhale.mlops.api.protocol.swmp.ClientSwmpRequest;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVersionVo;
import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVo;
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
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swmp.bo.SwmpQuery;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersion;
import ai.starwhale.mlops.domain.swmp.bo.SwmpVersionQuery;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageVersionMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class SwModelPackageServiceTest {

    private SwModelPackageService service;
    private SwModelPackageMapper swmpMapper;
    private SwModelPackageVersionMapper swmpVersionMapper;
    private SwmpConvertor swmpConvertor;
    private SwmpVersionConvertor versionConvertor;
    private StoragePathCoordinator storagePathCoordinator;
    private StorageAccessService storageAccessService;
    private StorageService storageService;
    private UserService userService;
    private ProjectManager projectManager;
    private SwmpManager swmpManager;
    private HotJobHolder jobHolder;
    private BundleManager bundleManager;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        swmpMapper = mock(SwModelPackageMapper.class);
        swmpVersionMapper = mock(SwModelPackageVersionMapper.class);
        swmpConvertor = mock(SwmpConvertor.class);
        given(swmpConvertor.convert(any(SwModelPackageEntity.class)))
                .willAnswer(invocation -> {
                    SwModelPackageEntity entity = invocation.getArgument(0);
                    return SwModelPackageVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(SwmpVersionConvertor.class);
        given(versionConvertor.convert(any(SwModelPackageVersionEntity.class)))
                .willAnswer(invocation -> {
                    SwModelPackageVersionEntity entity = invocation.getArgument(0);
                    return SwModelPackageVersionVo.builder()
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
        swmpManager = mock(SwmpManager.class);
        jobHolder = mock(HotJobHolder.class);

        service = new SwModelPackageService(
                swmpMapper,
                swmpVersionMapper,
                new IdConvertor(),
                new VersionAliasConvertor(),
                swmpConvertor,
                versionConvertor,
                storagePathCoordinator,
                swmpManager,
                storageAccessService,
                storageService,
                userService,
                projectManager,
                jobHolder
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
        given(swmpMapper.listSwModelPackages(same(1L), anyString()))
                .willReturn(List.of(
                        SwModelPackageEntity.builder().id(1L).build(),
                        SwModelPackageEntity.builder().id(2L).build()
                ));
        var res = service.listSwmp(SwmpQuery.builder()
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
    public void testDeleteSwmp() {
        RemoveManager removeManager = mock(RemoveManager.class);
        given(removeManager.removeBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "m1")
        ))).willReturn(true);
        try (var mock = mockStatic(RemoveManager.class)) {
            mock.when(() -> RemoveManager.create(any(), any()))
                    .thenReturn(removeManager);
            var res = service.deleteSwmp(SwmpQuery.builder().projectUrl("p1").swmpUrl("m1").build());
            assertThat(res, is(true));

            res = service.deleteSwmp(SwmpQuery.builder().projectUrl("p2").swmpUrl("m2").build());
            assertThat(res, is(false));
        }
    }

    @Test
    public void testRecoverSwmp() {
        RecoverManager recoverManager = mock(RecoverManager.class);
        given(recoverManager.recoverBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "m1")
        ))).willReturn(true);
        try (var mock = mockStatic(RecoverManager.class)) {
            mock.when(() -> RecoverManager.create(any(), any(), any()))
                    .thenReturn(recoverManager);

            var res = service.recoverSwmp("p1", "m1");
            assertThat(res, is(true));

            res = service.recoverSwmp("p1", "m2");
            assertThat(res, is(false));
        }
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
    public void testListSwmpInfo() {
        given(swmpMapper.findByName(same("m1"), same(1L)))
                .willReturn(SwModelPackageEntity.builder().id(1L).build());
        given(swmpVersionMapper.listVersions(same(1L), any(), any()))
                .willReturn(List.of(SwModelPackageVersionEntity.builder().versionOrder(2L).build()));

        var res = service.listSwmpInfo("1", "m1");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        given(projectManager.findByNameOrDefault(same("1"), same(1L)))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(swmpMapper.listSwModelPackages(same(1L), any()))
                .willReturn(List.of(SwModelPackageEntity.builder().id(1L).build()));

        res = service.listSwmpInfo("1", "");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        assertThrows(StarwhaleApiException.class,
                () -> service.listSwmpInfo("2", "m1"));
    }

    @Test
    public void testGetSwmpInfo() {
        given(swmpMapper.findSwModelPackageById(same(1L)))
                .willReturn(SwModelPackageEntity.builder().id(1L).build());

        given(swmpMapper.findSwModelPackageById(same(2L)))
                .willReturn(SwModelPackageEntity.builder().id(2L).build());

        assertThrows(StarwhaleApiException.class,
                () -> service.getSwmpInfo(SwmpQuery.builder().projectUrl("1").swmpUrl("3").build()));

        given(swmpVersionMapper.findVersionById(same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().id(1L).versionOrder(2L).build());

        var res = service.getSwmpInfo(SwmpQuery.builder()
                .projectUrl("1")
                .swmpUrl("1")
                .swmpVersionUrl("1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        given(swmpVersionMapper.getLatestVersion(same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().id(1L).versionOrder(2L).build());

        res = service.getSwmpInfo(SwmpQuery.builder()
                .projectUrl("1")
                .swmpUrl("1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        assertThrows(StarwhaleApiException.class,
                () -> service.getSwmpInfo(SwmpQuery.builder().projectUrl("1").swmpUrl("2").build()));
    }

    @Test
    public void testModifySwmpVersion() {
        given(swmpVersionMapper.update(argThat(entity -> entity.getId() == 1L)))
                .willReturn(1);

        var res = service.modifySwmpVersion("1", "1", "1", new SwmpVersion());
        assertThat(res, is(true));

        res = service.modifySwmpVersion("1", "1", "2", new SwmpVersion());
        assertThat(res, is(false));
    }

    @Test
    public void testListSwmpVersionHistory() {
        given(swmpVersionMapper.listVersions(anyLong(), anyString(), anyString()))
                .willReturn(List.of(SwModelPackageVersionEntity.builder().id(1L).swmpName("m1").build()));
        var res = service.listSwmpVersionHistory(
                SwmpVersionQuery.builder()
                        .projectUrl("1")
                        .swmpUrl("1")
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
    public void testFindModelByVersionId() {
        given(swmpVersionMapper.findVersionsByIds(anyList()))
                .willReturn(List.of(
                        SwModelPackageVersionEntity.builder().swmpId(1L).build(),
                        SwModelPackageVersionEntity.builder().swmpId(2L).build()
                ));

        given(swmpMapper.findSwModelPackagesByIds(anyList()))
                .willAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> SwModelPackageEntity.builder().id(id).build())
                            .collect(Collectors.toList());
                });

        var res = service.findModelByVersionId(List.of());
        assertThat(res, allOf(
                iterableWithSize(2),
                hasItem(hasProperty("id", is("1"))),
                hasItem(hasProperty("id", is("2")))
        ));
    }

    @Test
    public void testUpload() {
        given(projectManager.getProject(anyString()))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(swmpMapper.findByNameForUpdate(anyString(), same(1L)))
                .willReturn(SwModelPackageEntity.builder().id(1L).build());
        given(swmpVersionMapper.findByNameAndSwmpId(anyString(), same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder()
                        .id(1L)
                        .storagePath("path1")
                        .evalJobs("job1")
                        .build());
        given(jobHolder.ofStatus(anySet()))
                .willReturn(List.of(
                        Job.builder().swmp(SwModelPackage.builder().name("m1").version("v1").build()).build(),
                        Job.builder().swmp(SwModelPackage.builder().name("m2").version("v2").build()).build()
                ));
        given(storagePathCoordinator.generateSwmpPath(any(), any(), any()))
                .willReturn("path2");

        try (var mock = mockStatic(TarFileUtil.class)) {
            mock.when(() -> TarFileUtil.getContentFromTarFile(any(), any(), any()))
                    .thenReturn(new byte[]{1});

            ClientSwmpRequest request = new ClientSwmpRequest();
            request.setProject("1");
            request.setSwmp("m1:v1");

            MultipartFile dsFile = new MockMultipartFile("dsFile", new byte[10]);
            assertThrows(StarwhaleApiException.class, () -> service.upload(dsFile, request));

            request.setForce("1");
            assertThrows(StarwhaleApiException.class, () -> service.upload(dsFile, request));

            request.setSwmp("m3:v3");
            service.upload(dsFile, request);

            request.setProject("2");
            service.upload(dsFile, request);
        }
    }

    @Test
    public void testPull() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(swmpMapper.findByName(same("m1"), same(1L)))
                .willReturn(SwModelPackageEntity.builder().id(1L).build());
        assertThrows(SwValidationException.class,
                () -> service.pull("2", "m2", "v2", response));

        given(swmpVersionMapper.findByNameAndSwmpId(same("v1"), same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().storagePath("path1").build());
        assertThrows(SwValidationException.class,
                () -> service.pull("1", "m1", "v4", response));

        given(swmpVersionMapper.findByNameAndSwmpId(same("v2"), same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().storagePath("path2").build());

        given(swmpVersionMapper.findByNameAndSwmpId(same("v3"), same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().storagePath("path3").build());

        given(storageAccessService.list(anyString())).willThrow(IOException.class);
        given(storageAccessService.list(same("path1"))).willReturn(Stream.of("path1/file1"));
        given(storageAccessService.list(same("path2"))).willReturn(Stream.of());
        assertThrows(SwValidationException.class,
                () -> service.pull("1", "m1", "v2", response));
        assertThrows(SwProcessException.class,
                () -> service.pull("1", "m1", "v3", response));

        try (LengthAbleInputStream fileInputStream = mock(LengthAbleInputStream.class);
                ServletOutputStream outputStream = mock(ServletOutputStream.class)) {
            given(storageAccessService.get(anyString())).willReturn(fileInputStream);
            given(fileInputStream.transferTo(any())).willReturn(1000L);
            given(response.getOutputStream()).willReturn(outputStream);

            service.pull("1", "m1", "v1", response);
        }
    }

    @Test
    public void testQuery() {
        given(swmpMapper.findByName(same("m1"), same(1L)))
                .willReturn(SwModelPackageEntity.builder().id(1L).build());

        given(swmpVersionMapper.findByNameAndSwmpId(same("v1"), same(1L)))
                .willReturn(SwModelPackageVersionEntity.builder().id(1L).build());

        var res = service.query("1", "m1", "v1");
        assertThat(res, is(""));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "m2", "v1"));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "m1", "v2"));

    }

}
