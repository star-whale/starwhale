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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import ai.starwhale.mlops.storage.fs.FileStorageEnv.FileSystemEnvType;
import ai.starwhale.mlops.storage.s3.S3Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestStorageAccessParser {

    final String auths="USER.S3.REGION=region\n"
        + "USER.S3.ENDPOINT=endpoint\n"
        + "USER.S3.SECRET=secret\n"
        + "USER.S3.ACCESS_KEY=access_key\n"
        + "USER.S3.myname.ENDPOINT=endpoint1\n"
        + "USER.S3.myname.SECRET=secret1\n"
        + "USER.S3.MNIST.SECRET=\n"
        + "USER.S3.MYNAME.ACCESS_KEY=access_key1\n";

    @Test
    public void testDefaultService(){
        StorageAccessService defaultStorageAccessService = mock(StorageAccessService.class);
        SWDatasetVersionMapper swDatasetVersionMapper = mock(SWDatasetVersionMapper.class);
        when(swDatasetVersionMapper.getVersionById(1L)).thenReturn(SWDatasetVersionEntity.builder().id(1L).storageAuths(auths).build());
        when(swDatasetVersionMapper.getVersionById(2L)).thenReturn(SWDatasetVersionEntity.builder().id(2L).storageAuths("").build());
        StorageAccessParser storageAccessParser = new StorageAccessParser(defaultStorageAccessService,swDatasetVersionMapper);
        StorageAccessService storageAccessService = storageAccessParser.getStorageAccessServiceFromAuth(2L, "s3://renyanda/bdc/xyf",
            "myname");
        Assertions.assertEquals(defaultStorageAccessService,storageAccessService);
    }

    @Test
    public void testEnv2S3Config(){
        StorageAccessParser storageAccessParser = new StorageAccessParser(null,null);
        S3Config s3Config = storageAccessParser.env2S3Config(
            new StorageUri("s3://renyanda/bdc/xyf"), new FileStorageEnv(
                FileSystemEnvType.S3)
                .add("USER.S3.MYTEST.ENDPOINT", "endpoint")
                .add("USER.S3.URTEST.ENDPOINT","EDP")
                .add("USER.S4.mytest.endpoint","dpd")
                .add("USER.S3.mytest.SECRET","SCret")
                .add("USER.S3.mytest.ACCESS_KEY","ack")
                .add("USER.S3.mytest.BUCKET","bkt")
                .add("USER.S3.MYTEST.REGION","region"), "mytest");
        Assertions.assertEquals("renyanda",s3Config.getBucket());
        Assertions.assertEquals("ack",s3Config.getAccessKey());
        Assertions.assertEquals("SCret",s3Config.getSecretKey());
        Assertions.assertEquals("region",s3Config.getRegion());
    }
}
