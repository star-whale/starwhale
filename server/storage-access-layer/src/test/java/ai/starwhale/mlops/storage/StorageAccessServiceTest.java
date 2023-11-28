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

package ai.starwhale.mlops.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.baidu.StorageAccessServiceBos;
import ai.starwhale.mlops.storage.fs.FsConfig;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import ai.starwhale.mlops.storage.ksyun.StorageAccessServiceKsyun;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import ai.starwhale.mlops.storage.qcloud.StorageAccessServiceQcloud;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class StorageAccessServiceTest {

    @TempDir
    private File rootDir;

    @Container
    private static final S3MockContainer s3Mock =
            new S3MockContainer(System.getProperty("s3mock.version", "latest"))
                    .withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
                    .withInitialBuckets("s3,minio,aliyun");

    private final String prefix = "starwhale-ut-store/";

    @SneakyThrows
    private void clear(StorageAccessService storageAccessService) {
        for (var path : storageAccessService.list(this.prefix).collect(Collectors.toList())) {
            storageAccessService.delete(path);
        }
    }

    private void run(StorageAccessService storageAccessService, boolean testSignedUrl) throws Exception {
        this.clear(storageAccessService);
        try {
            storageAccessService.put(this.prefix + "t1", "c:t1".getBytes(StandardCharsets.UTF_8));
            storageAccessService.put(this.prefix + "t2", "c:t2".getBytes(StandardCharsets.UTF_8));
            storageAccessService.put(this.prefix + "t/t3", "c:t/t3".getBytes(StandardCharsets.UTF_8));
            storageAccessService.put(this.prefix + "d/a", "c:d/a".getBytes(StandardCharsets.UTF_8));
            assertThat(storageAccessService.list(this.prefix + "t").collect(Collectors.toList()),
                    is(List.of(this.prefix + "t/t3", this.prefix + "t1", this.prefix + "t2")));
            try (var in = storageAccessService.get(this.prefix + "t1")) {
                assertThat(in.readAllBytes(), is("c:t1".getBytes(StandardCharsets.UTF_8)));
            }
            try (var in = storageAccessService.get(this.prefix + "t/t3")) {
                assertThat(in.readAllBytes(), is("c:t/t3".getBytes(StandardCharsets.UTF_8)));
            }
            assertThat(storageAccessService.head(this.prefix + "t1").isExists(), is(true));
            assertThat(storageAccessService.head(this.prefix + "t1", true).getMd5sum(), is(DigestUtils.md5Hex("c:t1")));
            storageAccessService.delete(this.prefix + "t/t3");
            storageAccessService.delete(this.prefix + "t1");
            assertThat(storageAccessService.head(this.prefix + "t1").isExists(), is(false));
            assertThat(storageAccessService.list(this.prefix + "t").collect(Collectors.toList()),
                    is(List.of(this.prefix + "t2")));
            var data = new byte[20 * 1024 * 1024];
            var off5m = 5 * 1024 * 1024;
            for (int i = 0; i < data.length; ++i) {
                data[i] = (byte) i;
            }
            storageAccessService.put(this.prefix + "h1", new ByteArrayInputStream(data), data.length - 1);
            storageAccessService.put(this.prefix + "h2", new ByteArrayInputStream(data));
            storageAccessService.put(this.prefix + "h3", data);
            assertThat(storageAccessService.head(this.prefix + "h1").getContentLength(), is((long) data.length - 1));
            assertThat(storageAccessService.head(this.prefix + "h2").getContentLength(), is((long) data.length));
            for (var fn : List.of(this.prefix + "h1", this.prefix + "h2", this.prefix + "h3")) {
                try (var in = storageAccessService.get(fn, (long) (data.length - off5m), 100L)) {
                    assertThat(in.readAllBytes(),
                            is(Arrays.copyOfRange(data, data.length - off5m, data.length - off5m + 100)));
                }
            }
            assertThrows(FileNotFoundException.class, () -> storageAccessService.get(this.prefix + "non-exists"));
            assertThrows(FileNotFoundException.class,
                    () -> storageAccessService.get(this.prefix + "non-exists", 1L, 1L));
            assertThat(storageAccessService.list(this.prefix + "non-exists").collect(Collectors.toList()), is(empty()));
            if (testSignedUrl) {
                String path = this.prefix + "unit_test/x";
                String content = "hello word";
                storageAccessService.put(path, content.getBytes(StandardCharsets.UTF_8));
                String signedUrl = storageAccessService.signedUrl(path, 1000 * 60L);
                try (InputStream inputStream = new URL(signedUrl).openStream()) {
                    Assertions.assertEquals(content, new String(inputStream.readAllBytes()));
                }

                content = "testSignedPutUrl";
                signedUrl = storageAccessService.signedPutUrl(path, "text/plain", 1000 * 60L);
                var conn = (HttpURLConnection) new URL(signedUrl).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "text/plain");
                try (var out = conn.getOutputStream()) {
                    out.write(content.getBytes(StandardCharsets.UTF_8));
                }
                try (var in = conn.getInputStream()) {
                    in.readAllBytes();
                }
                Assertions.assertEquals(content, new String(storageAccessService.get(path).readAllBytes()));
            }
        } finally {
            this.clear(storageAccessService);
        }
    }

    @Test
    public void testFile() throws Exception {
        this.run(new StorageAccessServiceFile(new FsConfig(this.rootDir.getAbsolutePath(), "", "")), false);
    }

    @Test
    public void testS3() throws Exception {
        this.run(new StorageAccessServiceS3(
                        S3Config.builder()
                                .bucket("s3")
                                .accessKey("ak")
                                .secretKey("sk")
                                .region("us-west-1")
                                .endpoint(s3Mock.getHttpEndpoint())
                                .hugeFileThreshold(10 * 1024 * 1024)
                                .hugeFilePartSize(5 * 1024 * 1024)
                                .build()),
                true);
    }

    // test using real aliyun account
    @Test
    public void testAliyun() throws Exception {
        this.run(new StorageAccessServiceAliyun(
                        S3Config.builder()
                                .bucket("aliyun")
                                .accessKey("ak")
                                .secretKey("sk")
                                .endpoint(s3Mock.getHttpEndpoint())
                                .hugeFileThreshold(10 * 1024 * 1024)
                                .hugeFilePartSize(5 * 1024 * 1024)
                                .build()),
                true);
    }

    @Test
    public void testMinio() throws Exception {
        this.run(new StorageAccessServiceMinio(S3Config.builder()
                        .bucket("minio")
                        .accessKey("ak")
                        .secretKey("sk")
                        .endpoint(s3Mock.getHttpEndpoint())
                        .hugeFileThreshold(10 * 1024 * 1024)
                        .hugeFilePartSize(5 * 1024 * 1024)
                        .build()),
                true);
    }

    @Test
    public void testMemory() throws Exception {
        this.run(new StorageAccessServiceMemory(), false);
    }

    // test using real qcloud account
    @Disabled
    @Test
    public void testQcloud() throws Exception {
        this.run(new StorageAccessServiceQcloud(
                        S3Config.builder()
                                .bucket("")
                                .accessKey("")
                                .secretKey("")
                                .region("")
                                .endpoint("cos.ap-beijing.myqcloud.com")
                                .hugeFileThreshold(10 * 1024 * 1024)
                                .hugeFilePartSize(5 * 1024 * 1024)
                                .build()),
                true);
    }

    // test using real bos account
    @Disabled
    @Test
    public void testBos() throws Exception {
        this.run(new StorageAccessServiceBos(
                        S3Config.builder()
                                .bucket("")
                                .accessKey("")
                                .secretKey("")
                                .endpoint("bj.bcebos.com")
                                .hugeFileThreshold(10 * 1024 * 1024)
                                .hugeFilePartSize(5 * 1024 * 1024)
                                .build()),
                true);
    }

    // test using real ks3 account
    @Disabled
    @Test
    public void testKsyun() throws Exception {
        this.run(new StorageAccessServiceKsyun(
                        S3Config.builder()
                                .bucket("")
                                .accessKey("")
                                .secretKey("")
                                .endpoint("ks3-cn-beijing.ksyuncs.com")
                                .hugeFileThreshold(10 * 1024 * 1024)
                                .hugeFilePartSize(5 * 1024 * 1024)
                                .build()),
                true);
    }
}
