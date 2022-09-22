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

package ai.starwhale.mlops.storage.minio;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class StorageAccessServiceMinioTest {

    @Container
    private static final S3MockContainer s3Mock =
            new S3MockContainer(System.getProperty("s3mock.version", "latest"))
                    .withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
                    .withInitialBuckets("test");

    private StorageAccessServiceMinio minio;

    @BeforeEach
    public void setUp() {
        minio = new StorageAccessServiceMinio(S3Config.builder()
                .bucket("test")
                .accessKey("ak")
                .secretKey("sk")
                .region("us-west-1")
                .endpoint(s3Mock.getHttpEndpoint())
                .build());
    }

    @Test
    public void testPutAndGet() throws IOException {
        String path = "unit_test/x";
        String content = "hello word";
        minio.put(path, content.getBytes(StandardCharsets.UTF_8));
        LengthAbleInputStream lengthAbleInputStream = minio.get(path);
        Assertions.assertEquals(content.length(), lengthAbleInputStream.getSize());
        Assertions.assertEquals(content, new String(lengthAbleInputStream.readAllBytes()));
    }


    @Test
    public void testPutAndGetRange() throws IOException {
        String path = "unit_test/x/r";
        String content = "hello word";
        minio.put(path, content.getBytes(StandardCharsets.UTF_8));
        LengthAbleInputStream lengthAbleInputStream = minio.get(path, 3L, 4L);
        Assertions.assertEquals(4, lengthAbleInputStream.getSize());
        Assertions.assertEquals("lo w", new String(lengthAbleInputStream.readAllBytes()));
    }

    @Test
    public void testPutAndList() throws IOException {
        String path = "unit_test/y/z";
        String content = "hello word";
        minio.put(path, content.getBytes(StandardCharsets.UTF_8));
        minio.put(path + "1", content.getBytes(StandardCharsets.UTF_8));
        Stream<String> stream = minio.list("unit_test/y");
        List<String> list = stream.sorted().collect(Collectors.toList());
        Assertions.assertIterableEquals(list, List.of("unit_test/y/z", "unit_test/y/z1"));
    }

    @Test
    public void testPutAndDelete() throws IOException {
        String path = "unit_test/x/d";
        String content = "hello word";
        minio.put(path, content.getBytes(StandardCharsets.UTF_8));
        StorageObjectInfo objectInfo = minio.head(path);
        Assertions.assertTrue(objectInfo.isExists());

        minio.delete(path);
        objectInfo = minio.head(path);
        Assertions.assertFalse(objectInfo.isExists());
    }


}
