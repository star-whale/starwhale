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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.model.ModelUploadRequest;
import ai.starwhale.mlops.api.protocol.model.ModelVersionVo;
import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.storage.FileDesc;
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
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.domain.model.converter.ModelVersionVoConverter;
import ai.starwhale.mlops.domain.model.converter.ModelVoConverter;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionViewEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
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
    private ProjectService projectService;
    private ModelDao modelDao;
    private HotJobHolder jobHolder;
    private BundleManager bundleManager;
    private TrashService trashService;
    private JobSpecParser jobSpecParser;

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
        given(versionConvertor.convert(any(ModelVersionEntity.class), any()))
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
        projectService = mock(ProjectService.class);
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        given(projectService.getProjectId(same("2")))
                .willReturn(2L);
        modelDao = mock(ModelDao.class);
        jobHolder = mock(HotJobHolder.class);
        trashService = mock(TrashService.class);
        jobSpecParser = mock(JobSpecParser.class);

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
                projectService,
                jobHolder,
                trashService,
                jobSpecParser);
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
        given(modelMapper.list(same(1L), anyString(), any(), any()))
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
    public void testFindBo() {
        ModelEntity m1 = ModelEntity.builder().id(1L).modelName("model1").build();
        ModelVersionEntity v1 = ModelVersionEntity.builder()
                .id(2L)
                .modelId(3L)
                .versionName("v1")
                .modelName("model1")
                .ownerId(1L)
                .versionMeta("test_meta")
                .build();
        given(modelDao.getModel(same(1L)))
                .willReturn(m1);
        given(modelDao.getModelVersion(same("v1")))
                .willReturn(v1);
        given(modelDao.findVersionById(same(2L)))
                .willReturn(v1);

        var res = service.findModel(1L);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(1L)),
                hasProperty("name", is("model1"))
        ));

        var res1 = service.findModelVersion(2L);
        assertThat(res1, allOf(
                notNullValue(),
                hasProperty("id", is(v1.getId())),
                hasProperty("modelId", is(v1.getModelId())),
                hasProperty("name", is(v1.getVersionName())),
                hasProperty("ownerId", is(v1.getOwnerId())),
                hasProperty("meta", is(v1.getVersionMeta()))
        ));

        var res2 = service.findModelVersion("v1");
        assertThat(res2, equalTo(res1));
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
    public void testListModelInfo() throws IOException {
        given(modelMapper.findByName(same("m1"), same(1L), any()))
                .willReturn(ModelEntity.builder().id(1L).build());
        given(modelVersionMapper.list(same(1L), any(), any()))
                .willReturn(List.of(ModelVersionEntity.builder().versionOrder(2L).build()));
        var is = new LengthAbleInputStream(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), 0);
        given(storageAccessService.get(any())).willReturn(is);

        var res = service.listModelInfo("1", "m1");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        given(projectService.findProject(same("1")))
                .willReturn(Project.builder().id(1L).build());
        given(modelMapper.list(same(1L), any(), any(), any()))
                .willReturn(List.of(ModelEntity.builder().id(1L).build()));

        res = service.listModelInfo("1", "");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        assertThrows(SwNotFoundException.class,
                () -> service.listModelInfo("2", "m1"));
    }

    @Test
    public void testGetModelInfo() throws IOException {
        given(modelMapper.find(same(1L)))
                .willReturn(ModelEntity.builder().id(1L).build());

        given(modelMapper.find(same(2L)))
                .willReturn(ModelEntity.builder().id(2L).build());

        assertThrows(SwNotFoundException.class,
                () -> service.getModelInfo(ModelQuery.builder().projectUrl("1").modelUrl("m3").build()));

        given(modelVersionMapper.find(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).versionOrder(2L).build());

        given(modelVersionMapper.findByLatest(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).versionOrder(2L).build());

        var is = new LengthAbleInputStream(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), 0);
        given(storageAccessService.get(any())).willReturn(is);
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

        assertThrows(SwValidationException.class, () -> service.modifyModelVersion("1", "1", "v1", new ModelVersion()));

        var res = service.modifyModelVersion("1", "1", "v1", ModelVersion.builder().tag("v1").build());
        assertThat(res, is(true));

        res = service.modifyModelVersion("1", "1", "v2", ModelVersion.builder().tag("v1").build());
        assertThat(res, is(false));
    }

    @Test
    public void testListModelVersionHistory() throws IOException {
        given(modelVersionMapper.list(anyLong(), anyString(), anyString()))
                .willReturn(List.of(ModelVersionEntity.builder().id(1L).build()));
        var is = new LengthAbleInputStream(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), 0);
        given(storageAccessService.get(any())).willReturn(is);
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
                    String ids = invocation.getArgument(0);
                    return Stream.of(ids.split(","))
                            .map(id -> ModelEntity.builder().id(Long.valueOf(id)).build())
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
    public void testUpload() throws IOException {
        given(projectService.findProject(anyString()))
                .willReturn(Project.builder().id(1L).build());
        given(modelMapper.findByName(anyString(), same(1L), any()))
                .willReturn(ModelEntity.builder().id(1L).build());
        given(modelVersionMapper.findByNameAndModelId(anyString(), same(1L)))
                .willReturn(ModelVersionEntity.builder()
                        .id(1L)
                        .storagePath("path1")
                        .jobs("job1")
                        .build());
        given(jobHolder.ofStatus(anySet()))
                .willReturn(List.of(
                        Job.builder().model(Model.builder().name("m1").version("v1").build()).build(),
                        Job.builder().model(Model.builder().name("m2").version("v2").build()).build()
                ));

        try (var mockIOUtils = mockStatic(IOUtils.class); var mockTarFileUtil = mockStatic(TarFileUtil.class)) {
            mockIOUtils.when(() -> IOUtils.toString(any(InputStream.class), any(Charset.class)))
                    .thenReturn("");
            given(modelVersionMapper.find(3L))
                    .willReturn(ModelVersionEntity.builder()
                            .id(1L)
                            .storagePath("path1")
                            .status(ModelVersionEntity.STATUS_UN_AVAILABLE)
                            .build()
                    );

            ModelUploadRequest request = new ModelUploadRequest();
            request.setProject("1");
            request.setSwmp("m1:v1");

            // case1: upload manifest
            MultipartFile manifestFile = new MockMultipartFile("modelFile", new byte[10]);
            assertThrows(StarwhaleApiException.class, () -> service.uploadManifest(manifestFile, request));

            request.setForce("1");
            assertThrows(StarwhaleApiException.class, () -> service.uploadManifest(manifestFile, request));

            request.setSwmp("m3:v3");
            var manifestContent = "build:\n"
                    + "  os: Linux\n"
                    + "  sw_version: 0.0.0.dev0\n"
                    + "created_at: 2022-12-01 22:17:19 CST\n"
                    + "resources:\n"
                    + "- name: empty.pt\n"
                    + "  path: src/model/empty.pt\n"
                    + "  signature: 786a02f742015903c6\n"
                    + "version: kjvunxjq24iif5grsbazgae7xwbe3om7ogd65eey\n";
            mockIOUtils.when(() -> IOUtils.toString(any(InputStream.class), any(Charset.class)))
                    .thenReturn(manifestContent);

            service.uploadManifest(manifestFile, request);
            verify(storageAccessService, times(1)).put(eq("path1/_manifest.yaml"), any(InputStream.class), eq(10L));

            // case2: upload src.tar
            MultipartFile srcTar = new MockMultipartFile("src", "src.tar", null, new byte[20]);
            mockTarFileUtil.when(() -> TarFileUtil.getContentFromTarFile(any(), any(), any()))
                    .thenReturn("".getBytes());
            service.uploadSrc(3L, srcTar, request);
            mockTarFileUtil.verify(() -> TarFileUtil.extract(any(), any(ArchiveFileConsumer.class)), times(1));
            verify(storageAccessService, times(1)).put(eq("path1/src.tar"), any(InputStream.class), eq(20L));

            // case3: upload model
            given(storagePathCoordinator.allocateCommonModelPoolPath(any(), any()))
                    .willReturn("test/project/1/common-model/1");
            MultipartFile modelFile = new MockMultipartFile("model.pth", "src/model/model.pth", null, new byte[100]);
            service.uploadModel(3L, "123456", modelFile, request);
            verify(storageAccessService, times(1))
                    .put(eq("test/project/1/common-model/1"), any(InputStream.class), eq(100L));

            given(modelVersionMapper.find(3L))
                    .willReturn(ModelVersionEntity.builder()
                            .id(3L)
                            .status(ModelVersionEntity.STATUS_AVAILABLE)
                            .build()
                    );
            request.setProject("1");
            assertThrows(StarwhaleApiException.class, () -> service.uploadSrc(3L, manifestFile, request));
            assertThrows(StarwhaleApiException.class, () -> service.uploadModel(3L, "", manifestFile, request));

        }
    }

    @Test
    public void testPullFile() {
        HttpServletResponse response = new MockHttpServletResponse();
        var is = new LengthAbleInputStream(new ByteArrayInputStream(new byte[10]), 10);
        service.pullFile("mnist.pth", () -> is, response);
        assertThat("write to response", response.getHeader("Content-Length").equals("10"));
    }

    @Test
    public void testPullSrcTar() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();
        String fileName = "src.tar";

        // case1: oss error test
        var srcPath1 = "path1";
        given(storageAccessService.get(srcPath1 + "/" + fileName))
                .willReturn(new LengthAbleInputStream(new ByteArrayInputStream(new byte[]{}), 0));
        service.pullSrcTar(fileName, srcPath1, response);
        verify(storageAccessService, times(1)).get(any());
        verify(storageAccessService, times(0)).list(any());

        assertThat("response head", response.containsHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertThat("response head file name",
                response.getHeader(HttpHeaders.CONTENT_DISPOSITION),
                is("attachment; filename=\"" + fileName + "\"")
        );

    }

    @Test
    public void testPullSrcTarFromRecompress() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();
        String fileName = "src.tar";

        // case1: oss error test
        var srcPath1 = "path1";
        given(storageAccessService.get(srcPath1 + "/" + fileName)).willThrow(IOException.class);
        given(storageAccessService.list(srcPath1 + "/src")).willThrow(IOException.class);
        assertThrows(SwProcessException.class, () -> service.pullSrcTar(fileName, srcPath1, response));
        verify(storageAccessService, times(1)).list(srcPath1 + "/src");
        verify(storageAccessService, times(1)).get(srcPath1 + "/" + fileName);

        // case2: empty files test
        var srcPath2 = "path";
        given(storageAccessService.get(srcPath2 + "/" + fileName)).willThrow(IOException.class);
        given(storageAccessService.list(srcPath2 + "/src")).willReturn(Stream.of());
        assertThrows(SwValidationException.class, () -> service.pullSrcTar(fileName, srcPath2, response));
        verify(storageAccessService, times(1)).list(srcPath2 + "/src");
        verify(storageAccessService, times(1)).get(srcPath2 + "/" + fileName);

        // case3 : full workflow test
        try (MockedStatic<TarFileUtil> tarFileUtilMockedStatic = mockStatic(TarFileUtil.class)) {
            // case 1: download file
            given(storageAccessService.list(srcPath2 + "/src"))
                    .willReturn(Stream.of("path/src/file1", "path/src/file2"));

            service.pullSrcTar(fileName, srcPath2, response);

            tarFileUtilMockedStatic.verify(() -> TarFileUtil.archiveAndTransferTo(any(), any()), times(1));
        }
    }

    @Test
    public void testPull() throws IOException {
        var manifestContent = "build:\n"
                + "  os: Linux\n"
                + "  sw_version: 0.0.0.dev0\n"
                + "created_at: 2022-12-01 22:17:19 CST\n"
                + "resources:\n"
                + "- name: empty.pt\n"
                + "  duplicate_check: true\n"
                + "  path: src/model/empty.pt\n"
                + "  desc: MODEL\n"
                + "  signature: uuuuuuuuuu\n"
                + "- name: dataset.yaml\n"
                + "  duplicate_check: true\n"
                + "  path: src/src/dataset.yaml\n"
                + "  desc: SRC\n"
                + "  signature: 66666666666\n"
                + "version: m1\n";

        given(modelVersionMapper.find(same(1L))).willReturn(
                ModelVersionEntity.builder().storagePath("path1").build());
        given(modelVersionMapper.find(same(2L))).willReturn(
                ModelVersionEntity.builder().storagePath("path2").build());
        given(modelVersionMapper.find(same(3L))).willReturn(
                ModelVersionEntity.builder().storagePath("path3").build());
        // case 1: not exist version test
        var responseForManifest = new MockHttpServletResponse();
        assertThrows(BundleException.class,
                () -> service.pull(
                        FileDesc.MANIFEST, "_manifest.yaml", "_manifest.yaml", "", "1", "m1", "v4",
                        responseForManifest));
        // case 2: guess name and path error
        assertThrows(SwValidationException.class,
                () -> service.pull(
                        null, "", "", "", "1", "m1", "v1",
                        responseForManifest));
        var is = new ByteArrayInputStream(manifestContent.getBytes());
        var manifestInputStream = new LengthAbleInputStream(is, manifestContent.getBytes().length);
        manifestInputStream.mark(0);
        given(storageAccessService.get(any())).willReturn(manifestInputStream);
        assertThrows(SwNotFoundException.class,
                () -> service.pull(
                        null, "empty1.pt", "src/model/empty1.pt", "", "1", "m1", "v1",
                        responseForManifest));

        // case 3: validation error test
        given(storageAccessService.list("path1/src")).willReturn(Stream.of("path1/src/file1"));
        given(storageAccessService.list("path2/src")).willReturn(Stream.of());
        given(storageAccessService.list("path3/src")).willThrow(IOException.class);
        manifestInputStream.reset();
        service.pull(
                FileDesc.MANIFEST, "_manifest.yaml", "_manifest.yaml", "", "1", "m1", "v1",
                responseForManifest);
        assertThat("upload manifest to response",
                Objects.equals(responseForManifest.getHeader("Content-Length"),
                        String.valueOf(manifestContent.getBytes().length)));

        // case 4: pull model file
        var modelPath = "sw/controller/project/foo/model/iiiiii";
        given(projectService.findProject("foo")).willReturn(Project.builder().id(1L).name("foo").build());
        given(storagePathCoordinator.allocateCommonModelPoolPath(eq(1L), eq("iiiiii"))).willReturn(modelPath);
        given(storageAccessService.get(modelPath)).willThrow(IOException.class);
        var responseForModel = new MockHttpServletResponse();
        manifestInputStream.reset();
        assertThrows(SwProcessException.class,
                () -> service.pull(
                        FileDesc.MODEL, "empty.pt", "src/model/empty.pt", "iiiiii", "foo", "m1", "v3",
                        responseForModel));

        modelPath = "sw/controller/project/foo/model/uuuuuuuuuu";
        given(storagePathCoordinator.allocateCommonModelPoolPath(eq(1L), eq("uuuuuuuuuu"))).willReturn(modelPath);
        given(storageAccessService.get(modelPath)).willReturn(
                new LengthAbleInputStream(new ByteArrayInputStream(new byte[100]), 100));
        manifestInputStream.reset();
        service.pull(
                null, "empty.pt", "src/model/empty.pt", "uuuuuuuuuu", "foo", "m1", "v1",
                responseForModel
        );
        assertThat("upload model to response", Objects.equals(responseForModel.getHeader("Content-Length"), "100"));

        // case 5: pull src as tar
        var responseForNormal = new MockHttpServletResponse();
        try (MockedStatic<TarFileUtil> tarFileUtilMockedStatic = mockStatic(TarFileUtil.class)) {
            given(storageAccessService.get("path1/src.tar")).willThrow(IOException.class);
            given(storageAccessService.list("path1/src")).willReturn(Stream.of("path1/src/a.py", "path1/src/b.py"));
            manifestInputStream.reset();
            service.pull(
                    FileDesc.SRC_TAR, "src.tar", "", "", "foo", "m1", "v1",
                    responseForNormal);

            tarFileUtilMockedStatic.verify(() -> TarFileUtil.archiveAndTransferTo(any(), any()), times(1));
            assertThat("upload src.tar to response",
                    Objects.equals(responseForNormal.getHeader("Content-Length"), "0"));
        }
    }

    @Test
    public void testQuery() {
        given(modelVersionMapper.find(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(1L).versionName("v1").build());

        var res = service.query("p1", "m1", "v1");
        assertThat(res, is("v1"));

        assertThrows(SwNotFoundException.class,
                () -> service.query("p1", "m2", "v2"));

        assertThrows(SwNotFoundException.class,
                () -> service.query("p1", "m1", "v3"));

    }

    @Test
    public void testShareModelVersion() {
        service.shareModelVersion("1", "d1", "v1", true);
        service.shareModelVersion("1", "d1", "v1", false);
    }

    @Test
    public void testListModelVersionView() throws IOException {
        given(modelVersionMapper.findByLatest(same(1L)))
                .willReturn(ModelVersionEntity.builder().id(5L).build());
        given(modelVersionMapper.findByLatest(same(3L)))
                .willReturn(ModelVersionEntity.builder().id(2L).build());
        given(modelVersionMapper.listModelVersionViewByProject(same(1L)))
                .willReturn(List.of(
                    ModelVersionViewEntity.builder().id(5L).modelId(1L).versionOrder(4L).projectName("sw")
                        .userName("sw").shared(false).modelName("model1").storagePath("any").build(),
                    ModelVersionViewEntity.builder().id(4L).modelId(1L).versionOrder(2L).projectName("sw")
                        .userName("sw").shared(false).modelName("model1").storagePath("any").build(),
                    ModelVersionViewEntity.builder().id(3L).modelId(1L).versionOrder(3L).projectName("sw")
                        .userName("sw").shared(false).modelName("model1").storagePath("any").build(),
                    ModelVersionViewEntity.builder().id(2L).modelId(3L).versionOrder(2L).projectName("sw")
                        .userName("sw").shared(false).modelName("model3").storagePath("any").build(),
                    ModelVersionViewEntity.builder().id(1L).modelId(3L).versionOrder(1L).projectName("sw")
                        .userName("sw").shared(false).modelName("model3").storagePath("any").build()
                ));

        given(modelVersionMapper.listModelVersionViewByShared(same(1L)))
                .willReturn(List.of(
                    ModelVersionViewEntity.builder().id(8L).modelId(2L).versionOrder(3L).projectName("sw2")
                        .userName("sw2").shared(true).modelName("model2").storagePath("any").build(),
                    ModelVersionViewEntity.builder().id(7L).modelId(2L).versionOrder(2L).projectName("sw2")
                        .userName("sw2").shared(true).modelName("model2").storagePath("any").build(),
                    ModelVersionViewEntity.builder().id(6L).modelId(4L).versionOrder(3L).projectName("sw2")
                        .userName("sw2").shared(true).modelName("model4").storagePath("any").build()
                ));

        var res = service.listModelVersionView("1");
        assertEquals(4, res.size());
        assertEquals("model1", res.get(0).getModelName());
        assertEquals("model3", res.get(1).getModelName());
        assertEquals("model2", res.get(2).getModelName());
        assertEquals("model4", res.get(3).getModelName());
        assertEquals(3, res.get(0).getVersions().size());
        assertEquals(2, res.get(1).getVersions().size());
        assertEquals(2, res.get(2).getVersions().size());
        assertEquals(1, res.get(3).getVersions().size());
        assertEquals("latest", res.get(0).getVersions().get(0).getAlias());
        assertEquals("latest", res.get(1).getVersions().get(0).getAlias());
        assertEquals("v3", res.get(2).getVersions().get(0).getAlias());
        assertEquals("v3", res.get(3).getVersions().get(0).getAlias());
    }
}
