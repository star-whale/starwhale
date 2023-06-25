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

package ai.starwhale.mlops.storage.aliyun;

import ai.starwhale.mlops.storage.s3.S3Config;
import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class StorageAccessServiceAliyunTest {

    @Container
    private static final S3MockContainer s3Mock =
            new S3MockContainer(System.getProperty("s3mock.version", "latest"))
                    .withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
                    .withInitialBuckets("test");

    private StorageAccessServiceAliyun aliyun;

    @BeforeEach
    public void setUp() {
        aliyun = new StorageAccessServiceAliyun(S3Config.builder()
                .bucket("test")
                .accessKey("ak")
                .secretKey("sk")
                .region("us-west-1")
                .endpoint(s3Mock.getHttpEndpoint())
                .build());
    }

    @Test
    public void testSignedUrl() throws IOException {
        String path = "unit_test/x";
        String content = "hello word";
        aliyun.put(path, content.getBytes(StandardCharsets.UTF_8));
        String signedUrl = aliyun.signedUrl(path, 1000 * 60L);
        try (InputStream inputStream = new URL(signedUrl).openStream()) {
            Assertions.assertEquals(content, new String(inputStream.readAllBytes()));
        }
    }

    @Test
    public void testSignedPutUrl() throws IOException {
        String path = "unit_test/x";
        String content = "testSignedPutUrl";
        String signedUrl = aliyun.signedPutUrl(path, "text/plain", 1000 * 60L);
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
        Assertions.assertEquals(content, new String(aliyun.get(path).readAllBytes()));
    }
}
