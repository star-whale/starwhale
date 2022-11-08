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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.env.UserStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StorageAccessParserTest {

    final String auths = "USER.S3.REGION=region\n"
            + "USER.S3.ENDPOINT=endpoint\n"
            + "USER.S3.SECRET=secret\n"
            + "USER.S3.ACCESS_KEY=access_key\n"
            + "USER.S3.myname.ENDPOINT=endpoint1\n"
            + "USER.S3.myname.SECRET=secret1\n"
            + "USER.S3.MNIST.SECRET=\n"
            + "USER.S3.MYNAME.ACCESS_KEY=access_key1\n";

    StorageAccessService defaultStorageAccessService = mock(StorageAccessService.class);

    DatasetVersionMapper datasetVersionMapper = mock(DatasetVersionMapper.class);

    List<CompatibleStorageAccessServiceBuilder> compilableStorageAccessServiceBuilders;


    @BeforeEach
    public void setup() {
        CompatibleStorageAccessServiceBuilder builder1 = mock(CompatibleStorageAccessServiceBuilder.class);
        CompatibleStorageAccessServiceBuilder builder2 = mock(CompatibleStorageAccessServiceBuilder.class);
        compilableStorageAccessServiceBuilders = List.of(builder1, builder2);
    }

    @Test
    public void testDefaultService() {
        when(datasetVersionMapper.getVersionById(2L)).thenReturn(
                DatasetVersionEntity.builder().id(2L).storageAuths("").build());

        StorageAccessParser storageAccessParser = new StorageAccessParser(defaultStorageAccessService,
                compilableStorageAccessServiceBuilders);
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromUri(
                "s3://renyanda/bdc/xyf");
        Assertions.assertEquals(defaultStorageAccessService, storageAccessService);
    }

    @Test
    public void testCache() {
        when(datasetVersionMapper.getVersionById(1L)).thenReturn(
                DatasetVersionEntity.builder().id(1L).storageAuths(auths).build());
        UserStorageAccessServiceBuilder userStorageAccessServiceBuilder = mock(UserStorageAccessServiceBuilder.class);
        StorageAccessServiceMinio storageAccessServiceMinio = mock(StorageAccessServiceMinio.class);
        when(userStorageAccessServiceBuilder.build(any(), any(), any())).thenReturn(storageAccessServiceMinio);
        StorageAccessParser storageAccessParser = new StorageAccessParser(defaultStorageAccessService,
                compilableStorageAccessServiceBuilders);

        StorageAccessService myname = storageAccessParser.getStorageAccessServiceFromUri("s3://renyanda/bdc/xyf");
        Assertions.assertEquals(storageAccessServiceMinio, myname);
        myname = storageAccessParser.getStorageAccessServiceFromUri("s3://renyanda/bdc/xyfzzz");
        Assertions.assertEquals(storageAccessServiceMinio, myname);
        verify(userStorageAccessServiceBuilder).build(any(), any(), any());
    }
}
