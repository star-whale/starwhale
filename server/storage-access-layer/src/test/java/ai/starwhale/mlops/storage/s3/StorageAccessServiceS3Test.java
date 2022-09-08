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
import static org.hamcrest.Matchers.is;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Testcontainers
public class StorageAccessServiceS3Test {

    @Container
    private static final S3MockContainer s3Mock =
            new S3MockContainer(System.getProperty("s3mock.version", "latest"))
                    .withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
                    .withInitialBuckets("test");

    private StorageAccessServiceS3 s3;

    @BeforeEach
    public void setUp() throws IOException {
        var client = S3Client.builder()
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .endpointOverride(URI.create(s3Mock.getHttpEndpoint()))
                .region(Region.of("us-west-1"))
                .build();
        client.putObject(PutObjectRequest.builder().bucket("test").key("t1").build(), RequestBody.fromString("a"));
        client.putObject(PutObjectRequest.builder().bucket("test").key("t2").build(), RequestBody.fromString("b"));
        client.putObject(PutObjectRequest.builder().bucket("test").key("t/1").build(), RequestBody.fromString("c"));
        client.putObject(PutObjectRequest.builder().bucket("test").key("t/2").build(), RequestBody.fromString("d"));
        client.putObject(PutObjectRequest.builder().bucket("test").key("x").build(), RequestBody.fromString("abcde"));
        this.s3 = new StorageAccessServiceS3(
                new S3Config("test", "ak", "sk", "us-west-1", s3Mock.getHttpEndpoint()));
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
    public void testDelete() throws IOException {
        this.s3.delete("x");
        assertThat(this.s3.list("").collect(Collectors.toList()), containsInAnyOrder("t1", "t2", "t/1", "t/2"));
    }
}
