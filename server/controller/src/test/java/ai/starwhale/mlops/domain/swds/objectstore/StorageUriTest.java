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

package ai.starwhale.mlops.domain.swds.objectstore;

import ai.starwhale.mlops.storage.StorageUri;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StorageUriTest {


    /**
     * s3://username:password@127.0.0.1:29000@starwhale/project/2/dataset/11/x
     * s3://localhsot@starwhale/project/2/dataset/11/s
     * s3://starwhale/project/2/dataset/11/s /starwhale/project/2/dataset/11/d
     * starwhale/project/2/dataset/11/ab
     */

    @Test
    public void testS3_ip_userpassword() {
        StorageUri storageUri = new StorageUri(
                "s3://username:password@127.0.0.1:29000@starwhale/project/2/dataset/11/x");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertEquals("username", storageUri.getUsername());
        Assertions.assertEquals("password", storageUri.getPassword());
        Assertions.assertEquals("127.0.0.1", storageUri.getHost());
        Assertions.assertEquals(29000, storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/x", storageUri.getPath());
    }

    @Test
    public void testS3_ip() {
        StorageUri storageUri = new StorageUri("s3://127.0.0.1@starwhale/project/2/dataset/11/s");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertEquals("127.0.0.1", storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/s", storageUri.getPath());
    }

    @Test
    public void testS3_host() {
        StorageUri storageUri = new StorageUri("s3://starwhale.ai@starwhale/project/2/dataset/11/s");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertEquals("starwhale.ai", storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/s", storageUri.getPath());
    }

    @Test
    public void testS3_ip_port() {
        StorageUri storageUri = new StorageUri("s3://127.0.0.1:19000@starwhale/project/2/dataset/11/s");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertEquals("127.0.0.1", storageUri.getHost());
        Assertions.assertEquals(19000, storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/s", storageUri.getPath());
    }

    @Test
    public void testS3_host_userpassword() {
        StorageUri storageUri = new StorageUri("s3://username:password@starwhale.ai@starwhale/project/2/dataset/11/x");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertEquals("username", storageUri.getUsername());
        Assertions.assertEquals("password", storageUri.getPassword());
        Assertions.assertEquals("starwhale.ai", storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/x", storageUri.getPath());
    }

    @Test
    public void testS3_localhost_userpassword() {
        StorageUri storageUri = new StorageUri("s3://username:password@localhost@starwhale/project/2/dataset/11/x");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertEquals("username", storageUri.getUsername());
        Assertions.assertEquals("password", storageUri.getPassword());
        Assertions.assertEquals("localhost", storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/x", storageUri.getPath());
    }

    @Test
    public void testS3_3() {
        StorageUri storageUri = new StorageUri("s3://starwhale/project/2/dataset/11/s");
        Assertions.assertEquals("s3", storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertNull(storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/s", storageUri.getPath());
    }

    public void testHdfs() {
        StorageUri storageUri = new StorageUri("webhdfs://ai.starwhale:567/starwhale/project/2/dataset/11/s");
        Assertions.assertEquals("webhdfs", storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertEquals("ai.starwhale", storageUri.getHost());
        Assertions.assertEquals(567, storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/s", storageUri.getPath());
    }

    @Test
    public void test_relative_path() {
        StorageUri storageUri = new StorageUri("/starwhale/project/2/dataset/11/d");
        Assertions.assertNull(storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertNull(storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/d", storageUri.getPath());
    }

    @Test
    public void testRelativePath() {
        StorageUri storageUri = new StorageUri("starwhale/project/2/dataset/11/d");
        Assertions.assertNull(storageUri.getSchema());
        Assertions.assertNull(storageUri.getUsername());
        Assertions.assertNull(storageUri.getPassword());
        Assertions.assertNull(storageUri.getHost());
        Assertions.assertNull(storageUri.getPort());
        Assertions.assertEquals("starwhale", storageUri.getBucket());
        Assertions.assertEquals("project/2/dataset/11/d", storageUri.getPath());
    }

}
