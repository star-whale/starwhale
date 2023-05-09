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

package ai.starwhale.mlops.storage.s3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Testcontainers
public class StorageAccessServiceS3Test {

    @Container
    private static final S3MockContainer s3Mock =
            new S3MockContainer(System.getProperty("s3mock.version", "latest"))
                    .withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
                    .withInitialBuckets("test,huge");

    private StorageAccessServiceS3 s3;
    private S3Client client;

    @BeforeEach
    public void setUp() throws IOException {
        this.client = S3Client.builder()
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .endpointOverride(URI.create(s3Mock.getHttpEndpoint()))
                .region(Region.of("us-west-1"))
                .build();
        this.client.putObject(PutObjectRequest.builder().bucket("test").key("t1").build(), RequestBody.fromString("a"));
        this.client.putObject(PutObjectRequest.builder().bucket("test").key("t2").build(), RequestBody.fromString("b"));
        this.client.putObject(PutObjectRequest.builder().bucket("test").key("t/1").build(),
                RequestBody.fromString("c"));
        this.client.putObject(PutObjectRequest.builder().bucket("test").key("t/2").build(),
                RequestBody.fromString("d"));
        this.client.putObject(PutObjectRequest.builder().bucket("test").key("x").build(),
                RequestBody.fromString("abcde"));
        this.s3 = new StorageAccessServiceS3(
                S3Config.builder()
                        .bucket("test")
                        .accessKey("ak")
                        .secretKey("sk")
                        .region("us-west-1")
                        .endpoint(s3Mock.getHttpEndpoint())
                        .hugeFileThreshold(10 * 1024 * 1024)
                        .hugeFilePartSize(5 * 1024 * 1024)
                        .build());
    }

    @Test
    public void testClearPendingMultipartUploads() {
        for (int i = 0; i < 2999; ++i) {
            this.client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                    .bucket("test")
                    .key("m" + i)
                    .build());
        }
        assertThat(this.client.listMultipartUploads(ListMultipartUploadsRequest.builder()
                        .bucket("test")
                        .build()).uploads(),
                hasSize(2999));
        new StorageAccessServiceS3(
                S3Config.builder()
                        .bucket("test")
                        .accessKey("ak")
                        .secretKey("sk")
                        .region("us-west-1")
                        .endpoint(s3Mock.getHttpEndpoint())
                        .build());
        assertThat(this.client.listMultipartUploads(ListMultipartUploadsRequest.builder()
                        .bucket("test")
                        .build()).uploads(),
                empty());
    }

    @Test
    public void testHead() throws IOException {
        var info = this.s3.head("t1");
        assertThat(info.isExists(), is(true));
        assertThat(info.getContentLength(), is(1L));
    }

    @Test
    public void testList() {
        assertThat(this.s3.list("").collect(Collectors.toList()), containsInAnyOrder("t1", "t2", "t/1", "t/2", "x"));
        assertThat(this.s3.list("t").collect(Collectors.toList()), containsInAnyOrder("t1", "t2", "t/1", "t/2"));
        assertThat(this.s3.list("t/").collect(Collectors.toList()), containsInAnyOrder("t/1", "t/2"));
    }

    @Test
    public void testGet() throws IOException {
        assertThat(new String(this.s3.get("t1").readAllBytes()), is("a"));
    }

    @Test
    public void testGetRange() throws IOException {
        assertThat(new String(this.s3.get("x", 1L, 2L).readAllBytes()), is("bc"));
    }

    @Test
    public void testPut() throws IOException {
        this.s3.put("t1", "t".getBytes(StandardCharsets.UTF_8));
        assertThat(new String(this.s3.get("t1").readAllBytes()), is("t"));
    }

    @Test
    public void testPutInputStream() throws IOException {
        this.s3.put("t1", new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), 3);
        assertThat(new String(this.s3.get("t1").readAllBytes()), is("abc"));
    }

    @Test
    public void testPutHugeFile() throws IOException {
        var data = new byte[20 * 1024 * 1024];
        var off5m = 5 * 1024 * 1024;
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte) i;
        }
        this.s3.put("t1", new ByteArrayInputStream(data), data.length);
        var info = this.s3.head("t1");
        assertThat(this.s3.get("t1", (long) (data.length - off5m), 100L).readAllBytes(),
                is(Arrays.copyOfRange(data, data.length - off5m, data.length - off5m + 100)));
        // TODO: test the whole content after s3mock fix issue with chunk encoding enabled
    }

    @Test
    public void testDelete() throws IOException {
        this.s3.delete("x");
        assertThat(this.s3.list("").collect(Collectors.toList()), containsInAnyOrder("t1", "t2", "t/1", "t/2"));
    }

    @Test
    public void testListHugeNumberOfFiles() {
        var s3 = new StorageAccessServiceS3(
                S3Config.builder()
                        .bucket("huge")
                        .accessKey("ak")
                        .secretKey("sk")
                        .region("us-west-1")
                        .endpoint(s3Mock.getHttpEndpoint())
                        .hugeFileThreshold(10 * 1024 * 1024)
                        .hugeFilePartSize(5 * 1024 * 1024)
                        .build());

        final String prefix = "huge-number";
        // https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html#API_ListObjectsV2_RequestSyntax
        // By default the action returns up to 1,000 key names.
        // The response might contain fewer keys but will never contain more
        final int count = 1001;

        var expectedFiles = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            var path = prefix + "/" + i;
            expectedFiles.add(path);
            s3.put(path, new byte[]{7});
        }

        var resp = s3.list(prefix + "/").collect(Collectors.toList());
        assertThat(resp.size(), is(count));
        assertThat(resp, containsInAnyOrder(expectedFiles.toArray()));
    }

    @Test
    public void testSignedUrl() throws IOException {
        String signedUrl = s3.signedUrl("t1", 1000 * 60L);
        try (InputStream content = new URL(signedUrl).openStream()) {
            Assertions.assertEquals("a", new String(content.readAllBytes()));
        }
    }

    @Test
    public void testSignedPutUrl() throws IOException {
        String path = "x";
        String content = "testSignedPutUrl";
        String signedUrl = s3.signedPutUrl(path, 1000 * 60L);
        var conn = (HttpURLConnection) new URL(signedUrl).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        try (var out = conn.getOutputStream()) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        try (var in = conn.getInputStream()) {
            in.readAllBytes();
        }
        Assertions.assertEquals(content, new String(s3.get(path).readAllBytes()));
    }
}
