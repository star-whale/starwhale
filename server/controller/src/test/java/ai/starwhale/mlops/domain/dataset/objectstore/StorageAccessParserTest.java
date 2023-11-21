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

package ai.starwhale.mlops.domain.dataset.objectstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import ai.starwhale.mlops.domain.system.SystemSetting;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageConnectionToken;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StorageAccessParserTest {

    StorageAccessService defaultStorageAccessService = new StorageAccessServiceMemory();

    StorageAccessParser storageAccessParser;

    SystemSetting systemSetting = new SystemSetting();

    @BeforeEach
    public void setup() throws URISyntaxException {
        storageAccessParser = new StorageAccessParser(defaultStorageAccessService);
        systemSetting.setStorageSetting(Set.of(
                new StorageConnectionToken("memory", Map.of()),
                new StorageConnectionToken("fs", Map.of("rootDir", "/", "serviceProvider", "host")),
                new StorageConnectionToken("s3", Map.of(
                        "endpoint", "http://10.34.2.1:8080",
                        "region", "region",
                        "bucket", "b1",
                        "ak", "ak",
                        "sk", "sk")),
                new StorageConnectionToken("s3", Map.of(
                        "endpoint", "http://10.34.2.1:8080",
                        "region", "region",
                        "bucket", "b2",
                        "ak", "ak",
                        "sk", "sk")),
                new StorageConnectionToken("aliyun", Map.of(
                        "endpoint", "http://10.34.2.1:8080",
                        "bucket", "b1",
                        "ak", "ak",
                        "sk", "sk"))));
        storageAccessParser.onUpdate(systemSetting);
    }

    @Test
    public void testCache() throws URISyntaxException {
        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://renyanda/bdc/xyf")),
                is(this.defaultStorageAccessService));
        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("invalid://a/b/c")),
                is(this.defaultStorageAccessService));
        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("/a/b/c")),
                is(this.defaultStorageAccessService));

        StorageAccessService s3AccessService1 = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://10.34.2.1:8080/b1/p1"));
        StorageAccessService s3AccessService2 = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://10.34.2.1:8080/b1/p2"));
        StorageAccessService s3AccessService3 = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://10.34.2.1:8080/b2/p2"));
        assertThat(s3AccessService1, instanceOf(StorageAccessServiceS3.class));
        assertThat(s3AccessService1, is(s3AccessService2));
        assertThat(s3AccessService1, not(is(s3AccessService3)));

        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("file://b1/c/d")),
                instanceOf(StorageAccessServiceFile.class));

        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("oss://10.34.2.1:8080/b1/c/d")),
                instanceOf(StorageAccessServiceAliyun.class));
    }

    @Test
    public void testSettingUpdate() throws URISyntaxException {
        systemSetting.setStorageSetting(Set.of(
                new StorageConnectionToken("s3", Map.of(
                        "endpoint", "http://10.34.2.1:8080",
                        "region", "region",
                        "bucket", "b1",
                        "ak", "ak",
                        "sk", "sk")),
                new StorageConnectionToken("aliyun", Map.of(
                        "endpoint", "http://10.34.2.1:8080",
                        "bucket", "b2",
                        "ak", "ak",
                        "sk", "sk"))));
        storageAccessParser.onUpdate(systemSetting);
        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://10.34.2.1:8080/b1/p1")),
                instanceOf(StorageAccessServiceS3.class));
        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("oss://10.34.2.1:8080/b1/c/d")),
                is(this.defaultStorageAccessService));
        assertThat(storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("oss://10.34.2.1:8080/b2/c/d")),
                instanceOf(StorageAccessServiceAliyun.class));
    }
}
