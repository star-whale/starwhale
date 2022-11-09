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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSetting;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessService;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.autofit.StorageConnectionToken;
import ai.starwhale.mlops.storage.env.UserStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StorageAccessParserTest {

    List<CompatibleStorageAccessServiceBuilder> compilableStorageAccessServiceBuilders;

    StorageAccessService defaultStorageAccessService = mock(StorageAccessService.class);

    List<StorageUri> uris;

    StorageAccessParser storageAccessParser;

    CompatibleStorageAccessService sa = mock(CompatibleStorageAccessService.class);
    CompatibleStorageAccessService sb = mock(CompatibleStorageAccessService.class);

    CompatibleStorageAccessService sc = mock(CompatibleStorageAccessService.class);
    CompatibleStorageAccessService sa1 = mock(CompatibleStorageAccessService.class);
    CompatibleStorageAccessService sb1 = mock(CompatibleStorageAccessService.class);
    CompatibleStorageAccessService sc1 = mock(CompatibleStorageAccessService.class);

    SystemSetting systemSetting = new SystemSetting();


    @BeforeEach
    public void setup() throws URISyntaxException {
        uris = List.of(
                new StorageUri("s3://10.34.2.1:8080/b1/p1"),
                new StorageUri("s3://local.s3.amazonaws.com/b1/p1"),
                new StorageUri("minio://10.34.2.13:8080/b1/p1"),
                new StorageUri("oss://10.34.2.1:8080/b1/p1"),
                new StorageUri("aliyun://10.34.2.1:8080/b1/p1"),
                new StorageUri("ftp://10.34.2.1:8080/b1/p1"),
                new StorageUri("sftp://10.34.2.1:8080/b1/p1"),
                new StorageUri("http://10.34.2.1:8080/b1/p1"),
                new StorageUri("hdfs://10.34.2.1:8080/b1/p1"),
                new StorageUri("file://b1/p1")
        );
        CompatibleStorageAccessServiceBuilder builder1 = mock(CompatibleStorageAccessServiceBuilder.class);
        when(builder1.couldBuild("t1")).thenReturn(true);
        when(sa.compatibleWith(eq(uris.get(0)))).thenReturn(true);
        when(builder1.build(eq(Map.of("a", "a1")))).thenReturn(sa);

        when(sb.compatibleWith(eq(uris.get(1)))).thenReturn(true);
        when(builder1.build(eq(Map.of("b", "b1")))).thenReturn(sb);

        when(sc.compatibleWith(eq(uris.get(2)))).thenReturn(true);
        when(builder1.build(eq(Map.of("c", "c1")))).thenReturn(sc);

        CompatibleStorageAccessServiceBuilder builder2 = mock(CompatibleStorageAccessServiceBuilder.class);
        when(builder2.couldBuild("t2")).thenReturn(true);
        when(sa1.compatibleWith(eq(uris.get(3)))).thenReturn(true);
        when(builder2.build(eq(Map.of("a1", "a1")))).thenReturn(sa1);
        when(sb1.compatibleWith(eq(uris.get(4)))).thenReturn(true);
        when(builder2.build(eq(Map.of("b1", "b1")))).thenReturn(sb1);
        when(sc1.compatibleWith(eq(uris.get(5)))).thenReturn(true);
        when(builder2.build(eq(Map.of("c1", "c1")))).thenReturn(sc1);

        compilableStorageAccessServiceBuilders = List.of(builder1, builder2);
        storageAccessParser = new StorageAccessParser(defaultStorageAccessService,
                compilableStorageAccessServiceBuilders);
        systemSetting.setStorageSetting(Set.of(new StorageConnectionToken("t1", Map.of("a", "a1")),
                new StorageConnectionToken("t2", Map.of("a1", "a1"))));
        storageAccessParser.onUpdate(systemSetting);
    }

    @Test
    public void testDefaultService() throws URISyntaxException {
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://renyanda/bdc/xyf"));
        Assertions.assertEquals(defaultStorageAccessService, storageAccessService);
    }

    @Test
    public void testCache() throws URISyntaxException {

        StorageAccessService myname = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(sa, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://10.34.2.1:8080/b1/p2"));
        Assertions.assertEquals(sa, myname);

        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("oss://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(sa1, myname);

    }

    @Test
    public void testSettingUpdate() throws URISyntaxException {
        systemSetting.setStorageSetting(Set.of(new StorageConnectionToken("t1", Map.of("b", "b1")),
                new StorageConnectionToken("t2", Map.of("a1", "a1"))));
        storageAccessParser.onUpdate(systemSetting);

        StorageAccessService myname = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(defaultStorageAccessService, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://local.s3.amazonaws.com/b1/p1"));
        Assertions.assertEquals(sb, myname);

        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("oss://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(sa1, myname);

        storageAccessParser.onUpdate(systemSetting);

        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(defaultStorageAccessService, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://local.s3.amazonaws.com/b1/p1"));
        Assertions.assertEquals(sb, myname);

        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("oss://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(sa1, myname);
    }

    @Test
    public void testSettingUpdateCache() throws URISyntaxException {

        StorageAccessService myname = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("s3://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(sa, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://10.34.2.1:8080/b1/p2"));
        Assertions.assertEquals(sa, myname);

        systemSetting.setStorageSetting(Set.of(new StorageConnectionToken("t1", Map.of("b", "b1")),
                new StorageConnectionToken("t2", Map.of("a1", "a1"))));
        storageAccessParser.onUpdate(systemSetting);

        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://10.34.2.1:8080/b1/p1"));
        Assertions.assertEquals(defaultStorageAccessService, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("s3://10.34.2.1:8080/b1/p2"));
        Assertions.assertEquals(defaultStorageAccessService, myname);
    }

    @Test
    public void testSettingUpdateCacheDefault() throws URISyntaxException {

        StorageAccessService myname = storageAccessParser.getStorageAccessServiceFromUri(
                new StorageUri("minio://10.34.2.13:8080/b1/p1"));
        Assertions.assertEquals(defaultStorageAccessService, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("minio://10.34.2.13:8080/b1/p2"));
        Assertions.assertEquals(defaultStorageAccessService, myname);

        systemSetting.setStorageSetting(Set.of(new StorageConnectionToken("t1", Map.of("c", "c1")),
                new StorageConnectionToken("t2", Map.of("a1", "a1"))));
        storageAccessParser.onUpdate(systemSetting);

        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("minio://10.34.2.13:8080/b1/p1"));
        Assertions.assertEquals(sc, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri(new StorageUri("minio://10.34.2.13:8080/b1/p2"));
        Assertions.assertEquals(sc, myname);
    }
}
