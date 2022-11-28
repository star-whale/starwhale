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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.model.ClientModelRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.common.ArchiveFileConsumer;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TarFileUtil;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.bundle.BundleException;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.domain.model.converter.ModelVersionVoConverter;
import ai.starwhale.mlops.domain.model.converter.ModelVoConverter;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.trash.TrashService;
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
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class ModelServiceTest {

    private ModelService service;
    private ModelMapper modelMapper;
    private ModelVersionMapper modelVersionMapper;
    private ModelVoConverter modelConverter;
    private ModelVersionVoConverter versionConvertor;
    private StoragePathCoordinator storagePathCoordinator;
    private StorageAccessService storageAccessService;
    private StorageService storageService;
    private UserService userService;
    private ProjectManager projectManager;
    private ModelDao modelDao;
    private HotJobHolder jobHolder;
    private BundleManager bundleManager;
    private TrashService trashService;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        modelMapper = mock(ModelMapper.class);
        modelVersionMapper = mock(ModelVersionMapper.class);
        modelConverter = mock(ModelVoConverter.class);
        given(modelConverter.convert(any(ModelEntity.class)))
                .willAnswer(invocation -> {
                    ModelEntity entity = invocation.getArgument(0);
                    return ModelVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(ModelVersionVoConverter.class);
        given(versionConvertor.convert(any(ModelVersionEntity.class)))
                .willAnswer(invocation -> {
                    ModelVersionEntity entity = invocation.getArgument(0);
                    return ModelVersionVo.builder()
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
        modelDao = mock(ModelDao.class);
        jobHolder = mock(HotJobHolder.class);
        trashService = mock(TrashService.class);

        service = new ModelService(
                modelMapper,
                modelVersionMapper,
                new IdConverter(),
                new VersionAliasConverter(),
                modelConverter,
                versionConvertor,
                storagePathCoordinator,
                modelDao,
                storageAccessService,
                storageService,
                userService,
                projectManager,
                jobHolder,
                trashService);
        bundleManager = mock(BundleManager.class);
        given(bundleManager.getBundleId(any(BundleUrl.class)))
                .willAnswer(invocation -> {
                    BundleUrl bundleUrl = invocation.getArgument(0);
                    switch (bundleUrl.getBundleUrl()) {
                        case "m1":
                            return 1L;
                        case "m2":
                            return 2L;
                        case "m3":
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
    public void testListModel() {
        given(modelMapper.list(same(1L), anyString(), any()))
                .willReturn(List.of(
                        ModelEntity.builder().id(1L).build(),
                        ModelEntity.builder().id(2L).build()
                ));
        var res = service.listModel(ModelQuery.builder()
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
    public void testDeleteModel() {
        RemoveManager removeManager = mock(RemoveManager.class);
        given(removeManager.removeBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "m1")
        ))).willReturn(true);
        try (var mock = mockStatic(RemoveManager.class)) {
            mock.when(() -> RemoveManager.create(any(), any()))
                    .thenReturn(removeManager);
            var res = service.deleteModel(ModelQuery.builder().projectUrl("p1").modelUrl("m1").build());
            assertThat(res, is(true));

            res = service.deleteModel(ModelQuery.builder().projectUrl("p2").modelUrl("m2").build());
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
    public void testListModelInfo() {
        given(modelMapper.findByName(same("m1"), same(1L), any()))
                .willReturn(ModelEntity.builder().id(1L).build());
        given(modelVersionMapper.list(same(1L), any(), any()))
                .willReturn(List.of(ModelVersionEntity.builder().versionOrder(2L).build()));

        var res = service.listModelInfo("1", "m1");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        given(projectManager.getProject(same("1")))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(modelMapper.list(same(1L), any(), any()))
                .willReturn(List.of(ModelEntity.builder().id(1L).build()));

        res = service.listModelInfo("1", "");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        assertThrows(StarwhaleApiException.class,
                () -> service.listModelInfo("2", "m1"));
    }

    @Test
    public void testGetModelInfo() {
        given(modelMapper.find(same(1L)))
                .willReturn(ModelEntity.builder().id(1L).build());

        given(modelMapper.find(same(2L)))
                .willReturn(ModelEntity.builder().id(2L).build());

        assertThrows(StarwhaleApiException.class,
                () -> service.getModelInfo(ModelQuery.builder().projectUrl("1").modelUrl("m3").build()));

        given(modelVersionMapper.find(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).versionOrder(2L).build());

        given(modelVersionMapper.findByLatest(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).versionOrder(2L).build());

        var res = service.getModelInfo(ModelQuery.builder()
                .projectUrl("p1")
                .modelUrl("m1")
                .modelVersionUrl("v1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        given(modelVersionMapper.findByLatest(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).versionOrder(2L).build());

        res = service.getModelInfo(ModelQuery.builder()
                .projectUrl("p1")
                .modelUrl("m1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        assertThrows(BundleException.class,
                () -> service.getModelInfo(ModelQuery.builder().projectUrl("1").modelUrl("2").build()));
    }

    @Test
    public void testModifyModelVersion() {
        given(modelVersionMapper.update(argThat(entity -> entity.getId() == 1L)))
                .willReturn(1);

        var res = service.modifyModelVersion("1", "1", "v1", new ModelVersion());
        assertThat(res, is(true));

        res = service.modifyModelVersion("1", "1", "v2", new ModelVersion());
        assertThat(res, is(false));
    }

    @Test
    public void testListModelVersionHistory() {
        given(modelVersionMapper.list(anyLong(), anyString(), anyString()))
                .willReturn(List.of(ModelVersionEntity.builder().id(1L).modelName("m1").build()));
        var res = service.listModelVersionHistory(
                ModelVersionQuery.builder()
                        .projectUrl("1")
                        .modelUrl("m1")
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
        given(modelVersionMapper.findByIds(anyString()))
                .willReturn(List.of(
                        ModelVersionEntity.builder().modelId(1L).build(),
                        ModelVersionEntity.builder().modelId(2L).build()
                ));

        given(modelMapper.findByIds(anyString()))
                .willAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> ModelEntity.builder().id(id).build())
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
        given(modelMapper.findByName(anyString(), same(1L), any()))
                .willReturn(ModelEntity.builder().id(1L).build());
        given(modelVersionMapper.findByNameAndModelId(anyString(), same(1L)))
                .willReturn(ModelVersionEntity.builder()
                        .id(1L)
                        .storagePath("path1")
                        .evalJobs("job1")
                        .build());
        given(jobHolder.ofStatus(anySet()))
                .willReturn(List.of(
                        Job.builder().model(Model.builder().name("m1").version("v1").build()).build(),
                        Job.builder().model(Model.builder().name("m2").version("v2").build()).build()
                ));
        given(storagePathCoordinator.allocateModelPath(any(), any(), any()))
                .willReturn("path2");

        try (var mock = mockStatic(TarFileUtil.class)) {
            mock.when(() -> TarFileUtil.getContentFromTarFile(any(), any(), any()))
                    .thenReturn(new byte[]{1});

            ClientModelRequest request = new ClientModelRequest();
            request.setProject("1");
            request.setSwmp("m1:v1");

            MultipartFile dsFile = new MockMultipartFile("dsFile", new byte[10]);
            assertThrows(StarwhaleApiException.class, () -> service.upload(dsFile, request));

            request.setForce("1");
            assertThrows(StarwhaleApiException.class, () -> service.upload(dsFile, request));

            request.setSwmp("m3:v3");
            service.upload(dsFile, request);
            mock.verify(() -> TarFileUtil.extract(any(), any(ArchiveFileConsumer.class)), times(1));

            request.setProject("2");
            service.upload(dsFile, request);
            mock.verify(() -> TarFileUtil.extract(any(), any(ArchiveFileConsumer.class)), times(1 + 1));
        }
    }

    @Test
    public void testPull() throws IOException {
        given(modelVersionMapper.find(same(1L)))
                .willReturn(ModelVersionEntity.builder().storagePath("path1").build());

        given(modelVersionMapper.find(same(2L)))
                .willReturn(ModelVersionEntity.builder().storagePath("path2").build());

        given(modelVersionMapper.find(same(3L)))
                .willReturn(ModelVersionEntity.builder().storagePath("path3").build());

        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(BundleException.class,
                () -> service.pull("1", "m1", "v4", response));

        given(storageAccessService.list(anyString())).willThrow(IOException.class);
        given(storageAccessService.list(same("path1"))).willReturn(Stream.of("path1/file1"));
        given(storageAccessService.list(same("path2"))).willReturn(Stream.of());
        assertThrows(SwValidationException.class,
                () -> service.pull("1", "m1", "v2", response));
        assertThrows(SwProcessException.class,
                () -> service.pull("1", "m1", "v3", response));

        try (LengthAbleInputStream fileInputStream = mock(LengthAbleInputStream.class);
                ServletOutputStream outputStream = mock(ServletOutputStream.class);
                MockedStatic<TarFileUtil> tarFileUtilMockedStatic = mockStatic(TarFileUtil.class)) {
            // case 1: only download .swmp file
            given(storageAccessService.list(same("path1"))).willReturn(Stream.of("path1/123456.swmp"));
            given(storageAccessService.get(anyString())).willReturn(fileInputStream);
            given(fileInputStream.transferTo(any())).willReturn(1000L);
            given(response.getOutputStream()).willReturn(outputStream);

            service.pull("1", "m1", "v1", response);
            tarFileUtilMockedStatic.verify(() -> TarFileUtil.archiveAndTransferTo(any(), any()), times(0));
            verify(storageAccessService, times(1)).get("path1/123456.swmp");

            // case 2: download extract file
            given(storageAccessService.list(same("path1")))
                    .willReturn(Stream.of("path1/123456.py", "path1/model/u.pth"));
            service.pull("1", "m1", "v1", response);
            tarFileUtilMockedStatic.verify(() -> TarFileUtil.archiveAndTransferTo(any(), any()), times(1));
        }
    }

    @Test
    public void testQuery() {
        given(modelVersionMapper.find(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).build());

        var res = service.query("p1", "m1", "v1");
        assertThat(res, is(""));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("p1", "m2", "v2"));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("p1", "m1", "v3"));

    }

}
