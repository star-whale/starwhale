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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.model.CreateModelVersionRequest;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobRequest;
import ai.starwhale.mlops.api.protocol.model.InitUploadBlobResult.Status;
import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.storage.FileNode;
import ai.starwhale.mlops.api.protocol.storage.FileNode.Type;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.configuration.security.JwtLoginToken;
import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.blob.BlobService;
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.ft.FineTuneDomainService;
import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.model.ModelPackageStorage.CompressionAlgorithm;
import ai.starwhale.mlops.domain.model.ModelPackageStorage.FileType;
import ai.starwhale.mlops.domain.model.bo.ModelQuery;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.model.bo.ModelVersionQuery;
import ai.starwhale.mlops.domain.model.converter.ModelVersionVoConverter;
import ai.starwhale.mlops.domain.model.converter.ModelVoConverter;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.docker.ContainerRunMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinderSimpleImpl;
import ai.starwhale.mlops.schedule.impl.docker.log.RunLogCollectorFactoryDocker;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.impl.k8s.reporting.ResourceEventHolder;
import ai.starwhale.mlops.schedule.log.RunLogSaver;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ComponentScan(basePackages = {
        "ai.starwhale.mlops.common",
        "ai.starwhale.mlops.domain",
        "ai.starwhale.mlops.datastore",
        "ai.starwhale.mlops.schedule.reporting",
        "ai.starwhale.mlops.resulting",
        "ai.starwhale.mlops.configuration.security"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ModelServingService.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test.*")
        })
@Import({K8sJobTemplate.class, ResourceEventHolder.class, SimpleMeterRegistry.class, RunLogSaver.class,
        DockerClientFinderSimpleImpl.class,
        ContainerRunMapper.class,
        RunLogCollectorFactoryDocker.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ModelServiceTest extends MySqlContainerHolder {

    private static final StorageAccessServiceMemory storageAccessServiceMemory = new StorageAccessServiceMemory();
    private User user;

    @TestConfiguration
    public static class Config {

        @Bean
        StorageAccessService storageAccessService() {
            return storageAccessServiceMemory;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        StoragePathCoordinator storagePathCoordinator() {
            return new StoragePathCoordinator("/tt");
        }

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        AdminServerProperties adminServerProperties() {
            return new AdminServerProperties();
        }

        @Bean
        SecurityProperties securityProperties() {
            return new SecurityProperties();
        }

        @Bean
        HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
            return mock(HandlerMappingIntrospector.class);
        }

        @Bean
        SwTaskScheduler swTaskScheduler() {
            return mock(SwTaskScheduler.class);
        }

        @Bean
        K8sClient k8sClient() {
            return mock(K8sClient.class);
        }

        @Bean
        ModelServingService modelServingService() {
            return mock(ModelServingService.class);
        }

        @Bean
        RunExecutor runExecutor() {
            return mock(RunExecutor.class);
        }
    }

    @Autowired
    private UserService userService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelDao modelDao;
    @Autowired
    private StorageAccessService storageAccessService;

    @Autowired
    private FineTuneDomainService fineTuneDomainService;

    private static final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

    private static final LZ4Compressor lz4Compressor = lz4Factory.fastCompressor();

    private static class Link {

        private final FileType type;
        public final String target;

        public Link(ModelPackageStorage.FileType type, String target) {
            this.type = type;
            this.target = target;
        }
    }

    private class ModelBuilder {


        private final Map<String, Object> dataMap = new HashMap<>();
        private final ByteBuffer dataBuf = ByteBuffer.allocate(4 * 1024 * 1024);
        private final ArrayList<ModelPackageStorage.File.Builder> fileList = new ArrayList<>();
        private final Random random = new Random();

        public void add(String path, Object data) {
            var pathList = path.split("/");
            var current = dataMap;
            for (int i = 0; i < pathList.length; ++i) {
                var s = pathList[i];
                if (i == pathList.length - 1) {
                    current.put(s, data);
                } else {
                    //noinspection unchecked
                    current = (Map<String, Object>) current.computeIfAbsent(s, k -> new HashMap<String, Object>());
                }
            }
        }

        public byte[] generate(String path, int length, boolean compressible) {
            var b = new byte[length];
            if (!compressible) {
                this.random.nextBytes(b);
            } else {
                var words = new ArrayList<byte[]>();
                for (int i = 0; i < 300; ++i) {
                    int len = this.random.nextInt(10) + 3;
                    var word = new byte[len];
                    for (int j = 0; j < len; ++j) {
                        word[j] = (byte) (this.random.nextInt(26) + 'a');
                    }
                    words.add(word);
                }
                ByteBuffer buf = ByteBuffer.allocate(length + 20);
                do {
                    buf.put((byte) ' ');
                    buf.put(words.get(this.random.nextInt(words.size())));
                } while (buf.position() < length);
                buf.flip();
                buf.get(b);
            }
            byte checksum = 0;
            for (var bb : path.getBytes(StandardCharsets.UTF_8)) {
                checksum ^= bb;
            }
            for (int i = 1; i < b.length; ++i) {
                checksum ^= b[i];
            }
            b[0] = checksum;
            this.add(path, b);
            return b;
        }

        public byte[] generate(String path, int minLength, int maxLength, boolean compressible) {
            var length = this.random.nextInt(maxLength - minLength + 1) + minLength;
            return this.generate(path, length, compressible);
        }

        public void remove(String path) {
            var pathList = path.split("/");
            var current = dataMap;
            for (int i = 0; i < pathList.length; ++i) {
                var s = pathList[i];
                if (i == pathList.length - 1) {
                    current.remove(s);
                } else {
                    //noinspection unchecked
                    current = (Map<String, Object>) current.computeIfAbsent(s, k -> new HashMap<String, Object>());
                }
            }
        }

        public void clear() {
            this.dataMap.clear();
        }

        private String putBlob(byte[] b) throws IOException {
            return this.putBlob(b, 0, b.length);
        }

        private String putBlob(byte[] b, int offset, int length) throws IOException {
            var result = modelService.initUploadBlob(
                    new InitUploadBlobRequest(DigestUtils.md5Hex(new ByteArrayInputStream(b, offset, length)),
                            (long) b.length));
            if (result.getStatus() == Status.OK) {
                storageAccessService.put(result.getSignedUrl(), new ByteArrayInputStream(b, offset, length));
                return modelService.completeUploadBlob(result.getBlobId());
            } else {
                return result.getBlobId();
            }
        }

        private List<String> writeData(ModelPackageStorage.File.Builder file, byte[] b)
                throws IOException {
            var ret = new ArrayList<String>();
            if (b.length == 0) {
                return ret;
            }
            var buf = ByteBuffer.allocate(this.dataBuf.capacity());
            for (int i = 0; i < b.length; ) {
                int size = Math.min(65536, b.length - i);
                for (; ; ) {
                    try {
                        var compressed = lz4Compressor.compress(b, i, size, buf.array(), buf.position() + 2);
                        if (compressed > 65536) {
                            size -= 4096;
                            continue;
                        } else {
                            buf.put((byte) (compressed >> 8));
                            buf.put((byte) (compressed & 0xff));
                            buf.position(buf.position() + compressed);
                        }
                        break;
                    } catch (LZ4Exception e) {
                        if (i == 0) {
                            for (var j = 0; j < b.length; j += this.dataBuf.capacity()) {
                                size = Math.min(this.dataBuf.capacity(), b.length - j);
                                ret.add(this.putBlob(b, j, size));
                            }
                            return ret;
                        }
                        ret.add(this.putBlob(buf.array(), 0, buf.position()));
                        buf.clear();
                    }
                }
                i += size;
            }
            ret.add(this.putBlob(buf.array(), 0, buf.position()));
            file.setCompressionAlgorithm(CompressionAlgorithm.COMPRESSION_ALGORITHM_LZ4);
            return ret;
        }

        private void appendData(ModelPackageStorage.File.Builder file, byte[] b) throws IOException {
            for (; ; ) {
                try {
                    var compressed = lz4Compressor.compress(b, 0, b.length, this.dataBuf.array(),
                            this.dataBuf.position() + 2);
                    if (compressed + 2 < b.length) {
                        file.setBlobOffset(this.dataBuf.position());
                        file.setBlobSize(compressed + 2);
                        file.setCompressionAlgorithm(CompressionAlgorithm.COMPRESSION_ALGORITHM_LZ4);
                        this.dataBuf.put((byte) (compressed >> 8));
                        this.dataBuf.put((byte) (compressed & 0xff));
                        this.dataBuf.position(this.dataBuf.position() + compressed);
                    } else {
                        if (this.dataBuf.remaining() < b.length) {
                            this.flushDataBuf();
                        }
                        file.setBlobOffset(this.dataBuf.position());
                        file.setBlobSize(b.length);
                        this.dataBuf.put(b);
                    }
                    this.fileList.add(file);
                    return;
                } catch (LZ4Exception e) {
                    this.flushDataBuf();
                }
            }
        }

        private void flushDataBuf() throws IOException {
            var blobId = this.putBlob(this.dataBuf.array(), 0, this.dataBuf.position());
            this.fileList.forEach(file -> file.addBlobIds(blobId));
            this.dataBuf.clear();
            this.fileList.clear();
        }

        public String build() throws IOException {
            var nodeList = new ArrayList<Pair<ModelPackageStorage.File.Builder, Object>>();
            var root = ModelPackageStorage.File.newBuilder().setName("").setType(FileType.FILE_TYPE_DIRECTORY);
            nodeList.add(Pair.of(root, this.dataMap));
            for (int i = 0; i < nodeList.size(); ++i) {
                var node = nodeList.get(i);
                var f = node.getLeft();
                var data = node.getRight();
                if (data instanceof byte[]) {
                    var b = (byte[]) data;
                    f.setSize(b.length);
                    f.setMd5(ByteString.copyFrom(DigestUtils.md5(b)));
                    f.setType(FileType.FILE_TYPE_REGULAR);
                    if (b.length == 0) {
                        f.addBlobIds("");
                    } else if (b.length < 4096) {
                        this.appendData(f, b);
                    } else {
                        var blobIds = this.writeData(f, b);
                        if (blobIds.size() == 1) {
                            f.addBlobIds(blobIds.get(0));
                        } else {
                            f.setType(FileType.FILE_TYPE_HUGE);
                            f.setFromFileIndex(nodeList.size());
                            for (int j = 0; j < blobIds.size(); j += 100) {
                                int k = Math.min(j + 100, blobIds.size());
                                var child = ModelPackageStorage.File.newBuilder();
                                blobIds.subList(j, k).forEach(child::addBlobIds);
                                nodeList.add(Pair.of(child, null));
                            }
                            f.setToFileIndex(nodeList.size());
                        }
                    }
                } else if (data instanceof Map) {
                    f.setType(FileType.FILE_TYPE_DIRECTORY);
                    @SuppressWarnings("unchecked") var dir = new TreeMap<>((Map<String, Object>) data);
                    f.setFromFileIndex(nodeList.size());
                    dir.forEach((k, v) -> {
                        if (v instanceof Map) {
                            var child = ModelPackageStorage.File.newBuilder().setName(k);
                            nodeList.add(Pair.of(child, v));
                        }
                    });
                    dir.forEach((k, v) -> {
                        if (v instanceof byte[] || v instanceof Link) {
                            var child = ModelPackageStorage.File.newBuilder().setName(k);
                            nodeList.add(Pair.of(child, v));
                        }
                    });
                    f.setToFileIndex(nodeList.size());
                } else if (data instanceof Link) {
                    f.setType(((Link) data).type);
                    f.setLink(((Link) data).target);
                }
            }
            var firstMetaBlob = ModelPackageStorage.MetaBlob.newBuilder();
            if (this.dataBuf.position() < 4096) {
                firstMetaBlob.setData(ByteString.copyFrom(this.dataBuf.array(), 0, this.dataBuf.position()));
                this.fileList.forEach(file -> file.addBlobIds(""));
            } else {
                this.flushDataBuf();
            }
            var currentMetaBlob = firstMetaBlob;
            int sum = 0;
            for (int i = 0; i < nodeList.size(); ++i) {
                var file = nodeList.get(i).getLeft();
                var message = file.build();
                var size = CodedOutputStream.computeMessageSize(1, message);
                if (sum + size > 65536) {
                    if (currentMetaBlob != firstMetaBlob) {
                        firstMetaBlob.addMetaBlobIndexes(
                                ModelPackageStorage.MetaBlobIndex.newBuilder()
                                        .setBlobId(this.putBlob(currentMetaBlob.build().toByteArray()))
                                        .setLastFileIndex(i - 1));
                    }
                    currentMetaBlob = ModelPackageStorage.MetaBlob.newBuilder();
                    sum = 0;
                }
                sum += size;
                currentMetaBlob.addFiles(message);
            }
            if (currentMetaBlob != firstMetaBlob) {
                firstMetaBlob.addMetaBlobIndexes(
                        ModelPackageStorage.MetaBlobIndex.newBuilder()
                                .setBlobId(this.putBlob(currentMetaBlob.build().toByteArray()))
                                .setLastFileIndex(nodeList.size() - 1));
            }
            return this.putBlob(firstMetaBlob.build().toByteArray());
        }
    }

    private final byte[] readmeFile = "this is a readme file".getBytes(StandardCharsets.UTF_8);

    private byte[] modelFile;

    private String modelMd5;

    private byte[] tzFile;

    private String tzMd5;

    private byte[] file99;

    private byte[] fileS;

    @SneakyThrows
    @BeforeAll
    public void init() {
        this.user = this.userService.loadUserByUsername("test");
        this.setUp();
        this.projectService.createProject(Project.builder()
                .name("1")
                .owner(User.builder().id(this.user.getId()).build())
                .privacy(Privacy.PRIVATE)
                .description("")
                .build());

        var modelBuilder = new ModelBuilder();
        modelBuilder.add("src/.starwhale/jobs.yaml", "[]".getBytes(StandardCharsets.UTF_8));
        this.modelFile = modelBuilder.generate("model", 100 * 1024 * 1024, false);
        this.modelMd5 = DigestUtils.md5Hex(this.modelFile);
        modelBuilder.add("readme", this.readmeFile);

        this.file99 = modelBuilder.generate(
                IntStream.range(0, 100).mapToObj(String::valueOf).collect(Collectors.joining("/")),
                1000, false);

        for (int i = 0; i < 5000; ++i) {
            modelBuilder.generate("t/f" + i, 1, 10000, true);
        }
        for (int i = 0; i < 5000; ++i) {
            modelBuilder.generate("t/d" + i + "/x", 1, 10000, true);
        }
        this.tzFile = modelBuilder.generate("t/z", 10 * 1024 * 1024, true);
        this.tzMd5 = DigestUtils.md5Hex(this.tzFile);
        modelBuilder.add("t/empty", new byte[0]);

        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                for (int k = 0; k < 5; ++k) {
                    for (int l = 0; l < 5; ++l) {
                        for (int m = 0; m < 5; ++m) {
                            modelBuilder.generate("v/" + i + "/" + j + "/" + k + "/" + l + "/" + m,
                                    1, 10000, true);
                        }
                    }
                }
            }
        }

        modelBuilder.add("xx", new Link(FileType.FILE_TYPE_HARDLINK, "t1"));
        modelBuilder.add("yy", new Link(FileType.FILE_TYPE_SYMLINK, "t2"));
        this.modelService.createModelVersion("1", "m", "v1",
                new CreateModelVersionRequest(modelBuilder.build(), null, false));

        modelBuilder.generate("test", 10, 100, true);
        modelBuilder.remove("v/1/1/1/1/1");
        modelBuilder.generate("t/d3/x", 1, 10000, true);
        this.modelService.createModelVersion("1", "m", "v2",
                new CreateModelVersionRequest(modelBuilder.build(), null, false));

        this.modelService.createModelVersion("1", "m", "v3",
                new CreateModelVersionRequest(modelBuilder.build(), null, false));

        modelBuilder.remove("test");
        modelBuilder.remove("0/1/2/3/4");
        modelBuilder.add("0/1/2/3/5/6/7", "".getBytes(StandardCharsets.UTF_8));
        this.modelService.createModelVersion("1", "m", "v4",
                new CreateModelVersionRequest(modelBuilder.build(), null, false));

        modelBuilder.clear();

        this.fileS = modelBuilder.generate("s", 10, 100, true);

        modelBuilder.add("src/.starwhale/jobs.yaml", "[]".getBytes(StandardCharsets.UTF_8));
        this.modelService.createModelVersion("1", "m1", "v1",
                new CreateModelVersionRequest(modelBuilder.build(), null, false));
        this.modelService.createModelVersion("1", "m1", "v1",
                new CreateModelVersionRequest(modelBuilder.build(), null, true));
        assertThrows(StarwhaleApiException.class, () -> this.modelService.createModelVersion("1", "m1", "v1",
                new CreateModelVersionRequest(modelBuilder.build(), null, false)));
    }

    @AfterAll
    public void clear() {
        this.modelDao.remove(Long.parseLong(
                this.modelService.getModelInfo(ModelQuery.builder().projectUrl("1").modelUrl("m").build()).getId()));
        this.modelDao.remove(Long.parseLong(
                this.modelService.getModelInfo(ModelQuery.builder().projectUrl("1").modelUrl("m1").build()).getId()));
    }

    @BeforeEach
    public void setUp() {
        var token = new JwtLoginToken(this.user, "", List.of(new Role(0L, Role.NAME_OWNER, Role.CODE_OWNER)));
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    public void testListModel() {
        var res = modelService.listModel(ModelQuery.builder()
                .projectUrl("1")
                .name("")
                .build(), new PageParams(1, 5));
        assertThat(res, allOf(
                hasProperty("size", is(2)),
                hasProperty("list", hasItem(hasProperty("name", is("m")))),
                hasProperty("list", hasItem(hasProperty("name", is("m1"))))
        ));
        res.getList().forEach(m -> assertEquals(this.user.getName(), m.getOwner().getName()));
    }

    @Test
    public void testFindModel() {
        var modelInfo = modelService.getModelInfo("1", "m", "v1");
        var modelId = Long.parseLong(modelInfo.getId());
        var versionInfo = modelInfo.getVersionInfo();
        var versionId = Long.parseLong(versionInfo.getId());
        var res = modelService.findModel(modelId);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(modelId)),
                hasProperty("name", is("m"))
        ));

        assertThat(modelService.findModelVersion(versionInfo.getId()), allOf(
                notNullValue(),
                hasProperty("id", is(versionId)),
                hasProperty("modelId", is(modelId)),
                hasProperty("name", is("v1")),
                hasProperty("ownerId", is(2L))
        ));

        assertThat(modelService.findModelVersion("v2"), allOf(
                notNullValue(),
                hasProperty("modelId", is(modelId)),
                hasProperty("name", is("v2")),
                hasProperty("ownerId", is(2L))
        ));
    }

    @Test
    public void testDeleteModel() {
        modelService.deleteModel(ModelQuery.builder().projectUrl("1").modelUrl("m1").build());
        var res = modelService.listModel(ModelQuery.builder().projectUrl("1").build(), new PageParams(1, 5));
        assertThat(res, allOf(
                hasProperty("size", is(1)),
                hasProperty("list", hasItem(hasProperty("name", is("m"))))
        ));
    }

    @Test
    public void testRevertVersionTo() {
        modelService.revertVersionTo("1", "m", "v1");
        modelService.revertVersionTo("1", "m", "v2");
        var res = modelService.listModelVersionHistory(
                ModelVersionQuery.builder()
                        .projectUrl("1")
                        .modelUrl("m")
                        .build(),
                new PageParams(1, 10));
        assertThat(res, allOf(
                hasProperty("list", hasItems(
                        hasProperty("name", is("v1")),
                        hasProperty("name", is("v2")),
                        hasProperty("name", is("v3")),
                        hasProperty("name", is("v4")),
                        hasProperty("name", is("v1")),
                        hasProperty("name", is("v2"))))
        ));
    }

    @Test
    public void testListModelInfo() {
        var res = modelService.listModelInfo("1", "m1");
        assertEquals(1, res.size());
        assertEquals("m1", res.get(0).getName());
        assertEquals("v1", res.get(0).getVersionInfo().getAlias());

        res = modelService.listModelInfo("1", "");
        assertEquals(5, res.size());

        assertThrows(SwNotFoundException.class,
                () -> modelService.listModelInfo("1", "m2"));
        assertThrows(SwNotFoundException.class,
                () -> modelService.listModelInfo("2", "m"));
    }

    @Test
    public void testGetModelInfo() {
        assertThrows(SwNotFoundException.class,
                () -> modelService.getModelInfo(ModelQuery.builder().projectUrl("1").modelUrl("m3").build()));

        var res = modelService.getModelInfo(ModelQuery.builder()
                .projectUrl("1")
                .modelUrl("m")
                .modelVersionUrl("v1")
                .build());

        assertEquals("m", res.getName());
        assertEquals("v1", res.getVersionInfo().getAlias());

        res = modelService.getModelInfo(ModelQuery.builder()
                .projectUrl("1")
                .modelUrl("m1")
                .build());

        assertEquals("m1", res.getName());
        assertEquals("v1", res.getVersionInfo().getAlias());
    }

    @Test
    public void testModifyModelVersion() {
        var res = modelService.modifyModelVersion("1", "m", "v1", ModelVersion.builder().tag("a").build());
        assertThat(res, is(true));
    }

    @Test
    public void testListModelVersionHistory() {
        var res = modelService.listModelVersionHistory(
                ModelVersionQuery.builder()
                        .projectUrl("1")
                        .modelUrl("m")
                        .build(),
                PageParams.builder().build()
        );
        assertThat(res, allOf(hasProperty("list", iterableWithSize(4))));
        res.getList().forEach(v -> assertEquals(this.user.getName(), v.getOwner().getName()));
    }

    @Test
    public void testQuery() {
        var res = modelService.query("1", "m1", "v1");
        assertThat(res, is("v1"));

        assertThrows(SwNotFoundException.class,
                () -> modelService.query("1", "m2", "v2"));

        assertThrows(SwNotFoundException.class,
                () -> modelService.query("1", "m1", "v3"));
    }

    @Test
    public void testShareModelVersion() {
        var projectService = mock(ProjectService.class);
        var modelDao = mock(ModelDao.class);
        var versionAliasConverter = mock(VersionAliasConverter.class);
        var modelVersionMapper = mock(ModelVersionMapper.class);

        var svc = new ModelService(
                mock(ModelMapper.class),
                modelVersionMapper,
                mock(BundleVersionTagDao.class),
                new IdConverter(),
                versionAliasConverter,
                mock(ModelVoConverter.class),
                mock(ModelVersionVoConverter.class),
                modelDao,
                mock(UserService.class),
                projectService,
                mock(HotJobHolder.class),
                mock(TrashService.class),
                mock(JobSpecParser.class),
                mock(BlobService.class),
                fineTuneDomainService
        );

        // public project
        when(projectService.getProjectVo("pub")).thenReturn(ProjectVo.builder().id("1").privacy("PUBLIC").build());
        when(modelDao.findById(1L)).thenReturn(ModelEntity.builder().id(1L).build());
        when(versionAliasConverter.isVersionAlias("v1")).thenReturn(true);
        when(modelDao.findVersionByAliasAndBundleId("v1", 1L)).thenReturn(ModelVersionEntity.builder().id(2L).build());
        svc.shareModelVersion("pub", "1", "v1", true);
        verify(modelVersionMapper).updateShared(2L, true);
        svc.shareModelVersion("pub", "1", "v1", false);
        verify(modelVersionMapper).updateShared(2L, false);

        reset(modelVersionMapper);
        // private project can not share resources
        when(projectService.getProjectVo("private")).thenReturn(ProjectVo.builder().id("2").privacy("PRIVATE").build());
        assertThrows(SwValidationException.class, () -> svc.shareModelVersion("private", "1", "v1", true));
        assertThrows(SwValidationException.class, () -> svc.shareModelVersion("private", "1", "v1", false));
        verify(modelVersionMapper, never()).updateShared(any(), any());
    }

    @Test
    public void testListModelVersionView() {
        var res = modelService.listModelVersionView("1", true, true);
        assertEquals(2, res.size());
        assertThat(res.get(1), allOf(hasProperty("projectName", is("starwhale")),
                hasProperty("modelName", is("m"))));

        assertThat(res.get(0), allOf(hasProperty("projectName", is("starwhale")),
                hasProperty("modelName", is("m1")),
                hasProperty("versions", hasItems(
                        allOf(hasProperty("versionName", is("v1")),
                                hasProperty("alias", is("v1")),
                                hasProperty("latest", is(true)))))));

        assertThat(res.get(1).getVersions(), hasItems(
                allOf(hasProperty("versionName", is("v1")),
                        hasProperty("alias", is("v1")),
                        hasProperty("latest", is(false))),
                allOf(hasProperty("versionName", is("v2")),
                        hasProperty("alias", is("v2")),
                        hasProperty("latest", is(false))),
                allOf(hasProperty("versionName", is("v3")),
                        hasProperty("alias", is("v3")),
                        hasProperty("latest", is(false))),
                allOf(hasProperty("versionName", is("v4")),
                        hasProperty("alias", is("v4")),
                        hasProperty("latest", is(true)))));
    }

    @Test
    public void testUploadBlob() throws IOException {
        var data = "test";
        var result = this.modelService.initUploadBlob(new InitUploadBlobRequest(DigestUtils.md5Hex(data), 4L));
        assertThat(result.getStatus(), is(Status.OK));
        assertThat(result.getSignedUrl(), is("blob/" + result.getBlobId()));

        var result2 = this.modelService.initUploadBlob(new InitUploadBlobRequest(DigestUtils.md5Hex(data), 4L));
        assertThat(result2.getStatus(), is(Status.OK));
        assertThat(result2.getSignedUrl(), is("blob/" + result2.getBlobId()));

        this.storageAccessService.put(result.getSignedUrl(),
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        this.storageAccessService.put(result2.getSignedUrl(),
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));

        assertThat(this.modelService.completeUploadBlob(result.getBlobId()), is(result.getBlobId()));
        assertThat(this.modelService.completeUploadBlob(result2.getBlobId()), is(result.getBlobId()));
    }

    @Test
    public void testUploadBlobMultiThreads() throws InterruptedException {
        var threads = new Thread[100];
        var idSetList = new ArrayList<Set<String>>();
        for (int i = 0; i < threads.length; ++i) {
            var idSet = new HashSet<String>();
            idSetList.add(idSet);
            threads[i] = new Thread(() -> {
                var random = new Random();
                for (int j = 0; j < 100000; ++j) {
                    try {
                        var data = Ints.toByteArray(random.nextInt(10000));
                        var result = this.modelService.initUploadBlob(
                                new InitUploadBlobRequest(DigestUtils.md5Hex(data), 10L));
                        this.storageAccessService.put(result.getSignedUrl(), data);
                        idSet.add(this.modelService.completeUploadBlob(result.getBlobId()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            threads[i].start();
        }
        for (var thread : threads) {
            thread.join();
        }
        assertThat(idSetList.stream().flatMap(Collection::stream).distinct()
                        .map(id -> {
                            try {
                                return Ints.fromByteArray(
                                        this.storageAccessService.get("blob/" + id)
                                                .readAllBytes());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toSet()),
                is(IntStream.range(0, 10000).boxed().collect(Collectors.toSet())));
    }

    @Test
    public void testListFiles() {
        assertThat(this.modelService.listFiles("1", "m", "v1", "").getFiles(),
                hasItems(allOf(hasProperty("name", is("src")),
                                hasProperty("type", is(FileNode.Type.DIRECTORY))),
                        allOf(hasProperty("name", is("0")),
                                hasProperty("type", is(FileNode.Type.DIRECTORY))),
                        allOf(hasProperty("name", is("t")),
                                hasProperty("type", is(FileNode.Type.DIRECTORY))),
                        allOf(hasProperty("name", is("xx")),
                                hasProperty("type", is(FileNode.Type.FILE))),
                        allOf(hasProperty("name", is("yy")),
                                hasProperty("type", is(FileNode.Type.FILE))),
                        allOf(hasProperty("name", is("v")),
                                hasProperty("type", is(FileNode.Type.DIRECTORY))),
                        allOf(hasProperty("name", is("readme")),
                                hasProperty("type", is(Type.FILE)),
                                hasProperty("size", is(String.valueOf(this.readmeFile.length))),
                                hasProperty("signature", is(DigestUtils.md5Hex(this.readmeFile)))),
                        allOf(hasProperty("name", is("model")),
                                hasProperty("type", is(Type.FILE)),
                                hasProperty("size", is(String.valueOf(this.modelFile.length))),
                                hasProperty("signature", is(DigestUtils.md5Hex(this.modelFile))))));
        assertThat(this.modelService.listFiles("1", "m", "v1", "t").getFiles(),
                allOf(iterableWithSize(10002),
                        hasItem(allOf(hasProperty("name", is("d0")),
                                hasProperty("type", is(Type.DIRECTORY)))),
                        hasItem(allOf(hasProperty("name", is("d4999")),
                                hasProperty("type", is(Type.DIRECTORY)))),
                        hasItem(allOf(hasProperty("name", is("f0")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("f4999")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("z")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("empty")),
                                hasProperty("type", is(Type.FILE))))));
        assertThat(this.modelService.listFiles("1", "m", "v1",
                        IntStream.range(0, 99).mapToObj(String::valueOf).collect(Collectors.joining("/"))).getFiles(),
                hasItems(allOf(hasProperty("name", is("99")),
                        hasProperty("type", is(Type.FILE)))));
        assertThat(this.modelService.listFiles("1", "m", "v1", "v/1/2/3/4").getFiles(),
                allOf(hasItem(allOf(hasProperty("name", is("0")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("1")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("2")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("3")),
                                hasProperty("type", is(Type.FILE)))),
                        hasItem(allOf(hasProperty("name", is("4")),
                                hasProperty("type", is(Type.FILE))))));
    }

    @Test
    public void testGetFileData() throws IOException {
        assertThat(this.modelService.getFileData("1", "m", "v1", "readme").readAllBytes(),
                is(this.readmeFile));
        assertThat(this.modelService.getFileData("1", "m", "v1",
                                IntStream.range(0, 100)
                                        .mapToObj(String::valueOf)
                                        .collect(Collectors.joining("/")))
                        .readAllBytes(),
                is(this.file99));
        assertThat(DigestUtils.md5Hex(this.modelService.getFileData("1", "m", "v1", "t/z").readAllBytes()),
                is(this.tzMd5));
        assertThat(DigestUtils.md5Hex(this.modelService.getFileData("1", "m", "v1", "model").readAllBytes()),
                is(this.modelMd5));

        assertThat(this.modelService.getFileData("1", "m", "v1", "t/empty").readAllBytes(), is(new byte[0]));

        assertThat(this.modelService.getFileData("1", "m1", "v1", "s").readAllBytes(), is(this.fileS));
        assertThat(this.modelService.getFileData("1", "m", "v1", "xx").readAllBytes(),
                is("hardlink:t1".getBytes(StandardCharsets.UTF_8)));
        assertThat(this.modelService.getFileData("1", "m", "v1", "yy").readAllBytes(),
                is("symlink:t2".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testGetFileDataReleaseResource() throws Exception {
        var blobService = mock(BlobService.class);
        var modelService = new ModelService(
                mock(ModelMapper.class),
                mock(ModelVersionMapper.class),
                mock(BundleVersionTagDao.class),
                mock(IdConverter.class),
                mock(VersionAliasConverter.class),
                mock(ModelVoConverter.class),
                mock(ModelVersionVoConverter.class),
                mock(ModelDao.class),
                mock(UserService.class),
                mock(ProjectService.class),
                mock(HotJobHolder.class),
                mock(TrashService.class),
                mock(JobSpecParser.class),
                blobService,
                fineTuneDomainService
        );
        var file = ModelPackageStorage.File.newBuilder()
                .setSize(1L)
                .addBlobIds("1")
                .setCompressionAlgorithm(ModelPackageStorage.CompressionAlgorithm.COMPRESSION_ALGORITHM_LZ4)
                .build();

        var blob = mock(ModelPackageStorage.MetaBlob.class);
        var svc = spy(modelService);
        doReturn(List.of(file)).when(svc).getFile(any(), any());
        doReturn(blob).when(svc).getModelMetaBlob(any(), any(), any(), any());

        var originIs = new LengthAbleInputStream(new ByteArrayInputStream(new byte[]{0, 1, 3}), 3L);
        var mockIs = spy(originIs);
        doNothing().when(mockIs).close();

        when(blobService.readBlob(anyString(), anyLong(), anyLong())).thenReturn(mockIs);

        svc.getFileData("1", "m", "v1", "readme").close();
        verify(mockIs).close();
    }

    private void checkDiff(FileNode f, String path, List<String> added, List<String> deleted, List<String> modified) {
        switch (f.getFlag()) {
            case ADDED:
                assertThat(added, hasItem(path + f.getName()));
                break;
            case DELETED:
                assertThat(deleted, hasItem(path + f.getName()));
                break;
            case UPDATED:
                assertThat(modified, hasItem(path + f.getName()));
                break;
            case UNCHANGED:
                assertThat(added, not(hasItem(path + f.getName())));
                assertThat(deleted, not(hasItem(path + f.getName())));
                assertThat(modified, not(hasItem(path + f.getName())));
                break;
            default:
                break;
        }
        if (f.getType() == Type.DIRECTORY) {
            for (var child : f.getFiles()) {
                checkDiff(child, path + f.getName() + "/", added, deleted, modified);
            }
        }
    }

    @Test
    public void testGetModelDiff() {
        for (var f : this.modelService.getModelDiff("1", "m", "v1", "v2").get("compareVersion")) {
            this.checkDiff(f, "", List.of("test"), List.of("v/1/1/1/1/1"), List.of("t/d3/x"));
        }
        for (var f : this.modelService.getModelDiff("1", "m", "v2", "v3").get("compareVersion")) {
            this.checkDiff(f, "", List.of(), List.of(), List.of());
        }
        for (var f : this.modelService.getModelDiff("1", "m", "v3", "v4").get("compareVersion")) {
            this.checkDiff(f, "",
                    List.of("0/1/2/3/5", "0/1/2/3/5/6", "0/1/2/3/5/6/7"),
                    List.of("test", "0/1/2/3/4"),
                    List.of());
        }
    }

    @Test
    public void testBlobUsage() {
        assertThat(this.modelService.getModelMetaBlob("1", "m", "v2", ""),
                is(this.modelService.getModelMetaBlob("1", "m", "v3", "")));
    }
}
