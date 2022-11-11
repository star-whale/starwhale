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
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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

    private void run(StorageAccessService storageAccessService) throws Exception {
        storageAccessService.put("t1", "c:t1".getBytes(StandardCharsets.UTF_8));
        storageAccessService.put("t2", "c:t2".getBytes(StandardCharsets.UTF_8));
        storageAccessService.put("t/t3", "c:t/t3".getBytes(StandardCharsets.UTF_8));
        storageAccessService.put("d/a", "c:d/a".getBytes(StandardCharsets.UTF_8));
        assertThat(storageAccessService.list("t").collect(Collectors.toList()), is(List.of("t/t3", "t1", "t2")));
        try (var in = storageAccessService.get("t1")) {
            assertThat(in.readAllBytes(), is("c:t1".getBytes(StandardCharsets.UTF_8)));
        }
        try (var in = storageAccessService.get("t/t3")) {
            assertThat(in.readAllBytes(), is("c:t/t3".getBytes(StandardCharsets.UTF_8)));
        }
        assertThat(storageAccessService.head("t1").isExists(), is(true));
        storageAccessService.delete("t/t3");
        storageAccessService.delete("t1");
        assertThat(storageAccessService.head("t1").isExists(), is(false));
        assertThat(storageAccessService.list("t").collect(Collectors.toList()), is(List.of("t2")));
        var data = new byte[20 * 1024 * 1024];
        var off5m = 5 * 1024 * 1024;
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte) i;
        }
        storageAccessService.put("h1", new ByteArrayInputStream(data), data.length);
        storageAccessService.put("h2", new ByteArrayInputStream(data));
        for (var fn : List.of("h1")) {
            try (var in = storageAccessService.get(fn, (long) (data.length - off5m), 100L)) {
                assertThat(in.readAllBytes(),
                        is(Arrays.copyOfRange(data, data.length - off5m, data.length - off5m + 100)));
            }
        }
    }

    @Test
    public void testFile() throws Exception {
        this.run(new StorageAccessServiceFile(this.rootDir.getAbsolutePath(), ""));
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
                        .build()));
    }

    @Test
    public void testAliyun() throws Exception {
        this.run(new StorageAccessServiceAliyun(
                S3Config.builder()
                        .bucket("aliyun")
                        .accessKey("ak")
                        .secretKey("sk")
                        .region("us-west-1")
                        .endpoint(s3Mock.getHttpEndpoint())
                        .hugeFileThreshold(10 * 1024 * 1024)
                        .hugeFilePartSize(5 * 1024 * 1024)
                        .build()));
    }

    @Test
    public void testMinio() throws Exception {
        this.run(new StorageAccessServiceMinio(S3Config.builder()
                .bucket("minio")
                .accessKey("ak")
                .secretKey("sk")
                .region("us-west-1")
                .endpoint(s3Mock.getHttpEndpoint())
                .hugeFileThreshold(10 * 1024 * 1024)
                .hugeFilePartSize(5 * 1024 * 1024)
                .build()));
    }

    @Test
    public void testMemory() throws Exception {
        this.run(new StorageAccessServiceMemory());
    }
}
