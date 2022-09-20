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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.env.UserStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import org.junit.jupiter.api.Assertions;
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

    SwDatasetVersionMapper swDatasetVersionMapper = mock(SwDatasetVersionMapper.class);


    @Test
    public void testDefaultService() {
        when(swDatasetVersionMapper.getVersionById(2L)).thenReturn(
                SwDatasetVersionEntity.builder().id(2L).storageAuths("").build());
        StorageAccessParser storageAccessParser = new StorageAccessParser(defaultStorageAccessService,
                swDatasetVersionMapper, null);
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromAuth(2L,
                "s3://renyanda/bdc/xyf",
                "myname");
        Assertions.assertEquals(defaultStorageAccessService, storageAccessService);
    }

    @Test
    public void testCache() {
        when(swDatasetVersionMapper.getVersionById(1L)).thenReturn(
                SwDatasetVersionEntity.builder().id(1L).storageAuths(auths).build());
        UserStorageAccessServiceBuilder userStorageAccessServiceBuilder = mock(UserStorageAccessServiceBuilder.class);
        StorageAccessServiceMinio storageAccessServiceMinio = mock(StorageAccessServiceMinio.class);
        when(userStorageAccessServiceBuilder.build(any(), any(), any())).thenReturn(storageAccessServiceMinio);
        StorageAccessParser storageAccessParser = new StorageAccessParser(defaultStorageAccessService,
                swDatasetVersionMapper, userStorageAccessServiceBuilder);

        StorageAccessService myname = storageAccessParser.getStorageAccessServiceFromAuth(1L, "s3://renyanda/bdc/xyf",
                "myname");
        Assertions.assertEquals(storageAccessServiceMinio, myname);
        myname = storageAccessParser.getStorageAccessServiceFromAuth(1L, "s3://renyanda/bdc/xyfzzz",
                "myname");
        Assertions.assertEquals(storageAccessServiceMinio, myname);
        verify(userStorageAccessServiceBuilder).build(any(), any(), any());
    }
}
