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

package ai.starwhale.mlops.storage.env;

import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.env.StorageEnv.StorageEnvType;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StorageEnvsPropertiesConverterTest {

    @Test
    public void testUserEnv2Config() {
        StorageEnvsPropertiesConverter storageEnvsPropertiesConverter = new StorageEnvsPropertiesConverter(null);
        S3Config s3Config = storageEnvsPropertiesConverter.envToS3Config(new StorageEnv(
                StorageEnvType.S3)
                .add("USER.S3.MYTEST.ENDPOINT", "endpoint")
                .add("USER.S3.URTEST.ENDPOINT", "EDP")
                .add("USER.S4.mytest.endpoint", "dpd")
                .add("USER.S3.mytest.SECRET", "SCret")
                .add("USER.S3.mytest.ACCESS_KEY", "ack")
                .add("USER.S3.mytest.BUCKET", "bkt")
                .add("USER.S3.MYTEST.REGION", "region"), new StorageUri("s3://renyanda/bdc/xyf"), "mytest");
        Assertions.assertEquals("renyanda", s3Config.getBucket());
        Assertions.assertEquals("ack", s3Config.getAccessKey());
        Assertions.assertEquals("SCret", s3Config.getSecretKey());
        Assertions.assertEquals("region", s3Config.getRegion());
    }

    public static final String bucket = "bucket";
    public static final String accessKey = "accessKey";
    public static final String secretKey = "secretKey";
    public static final String region = "region";
    public static final String endpoint = "endpoint";
    public static final String prefixKey = "controller";

    private StorageProperties storageProperties;

    @BeforeEach
    public void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setS3Config(
                S3Config.builder()
                        .bucket(bucket)
                        .accessKey(accessKey)
                        .secretKey(secretKey)
                        .region(region)
                        .endpoint(endpoint)
                        .build());
        storageProperties.setPathPrefix(prefixKey);
    }

    @Test
    public void testAliyunPropertiesToEnvs() {
        storageProperties.setType("aliyun");
        StorageEnvsPropertiesConverter storageEnvsPropertiesConverter = new StorageEnvsPropertiesConverter(
                storageProperties);
        Map<String, StorageEnv> stringStorageEnvMap = storageEnvsPropertiesConverter.propertiesToEnvs();
        Assertions.assertEquals(1, stringStorageEnvMap.size());
        StorageEnv storageEnv = stringStorageEnvMap.get(StorageEnvType.ALIYUN.name());
        Assertions.assertEquals(StorageEnvType.ALIYUN.name(), storageEnv.getEnvs().get(S3Env.ENV_TYPE));
        Assertions.assertEquals(secretKey, storageEnv.getEnvs().get(S3Env.ENV_SECRET_KEY));
        Assertions.assertEquals(accessKey, storageEnv.getEnvs().get(S3Env.ENV_ACCESS_KEY));
        Assertions.assertEquals(endpoint, storageEnv.getEnvs().get(S3Env.ENV_ENDPOINT));
        Assertions.assertEquals(bucket, storageEnv.getEnvs().get(S3Env.ENV_BUCKET));
        Assertions.assertEquals(region, storageEnv.getEnvs().get(S3Env.ENV_REGION));
        Assertions.assertEquals(prefixKey, storageEnv.getEnvs().get(S3Env.ENV_KEY_PREFIX));
        Assertions.assertEquals("{\"addressing_style\": \"virtual\"}",
                storageEnv.getEnvs().get(AliyunEnv.ENV_EXTRA_S3_CONFIGS));
    }

    @Test
    public void testS3PropertiesToEnvs() {
        storageProperties.setType("s3");
        StorageEnvsPropertiesConverter storageEnvsPropertiesConverter = new StorageEnvsPropertiesConverter(
                storageProperties);
        Map<String, StorageEnv> stringStorageEnvMap = storageEnvsPropertiesConverter.propertiesToEnvs();
        Assertions.assertEquals(1, stringStorageEnvMap.size());
        StorageEnv storageEnv = stringStorageEnvMap.get(StorageEnvType.S3.name());
        Assertions.assertEquals(StorageEnvType.S3.name(), storageEnv.getEnvs().get(S3Env.ENV_TYPE));
        Assertions.assertEquals(secretKey, storageEnv.getEnvs().get(S3Env.ENV_SECRET_KEY));
        Assertions.assertEquals(accessKey, storageEnv.getEnvs().get(S3Env.ENV_ACCESS_KEY));
        Assertions.assertEquals(endpoint, storageEnv.getEnvs().get(S3Env.ENV_ENDPOINT));
        Assertions.assertEquals(bucket, storageEnv.getEnvs().get(S3Env.ENV_BUCKET));
        Assertions.assertEquals(region, storageEnv.getEnvs().get(S3Env.ENV_REGION));
        Assertions.assertEquals(prefixKey, storageEnv.getEnvs().get(S3Env.ENV_KEY_PREFIX));
    }

    @Test
    public void testMinioPropertiesToEnvs() {
        storageProperties.setType("minio");
        StorageEnvsPropertiesConverter storageEnvsPropertiesConverter = new StorageEnvsPropertiesConverter(
                storageProperties);
        Map<String, StorageEnv> stringStorageEnvMap = storageEnvsPropertiesConverter.propertiesToEnvs();
        Assertions.assertEquals(1, stringStorageEnvMap.size());
        StorageEnv storageEnv = stringStorageEnvMap.get(StorageEnvType.S3.name());
        Assertions.assertEquals(StorageEnvType.S3.name(), storageEnv.getEnvs().get(S3Env.ENV_TYPE));
        Assertions.assertEquals(secretKey, storageEnv.getEnvs().get(S3Env.ENV_SECRET_KEY));
        Assertions.assertEquals(accessKey, storageEnv.getEnvs().get(S3Env.ENV_ACCESS_KEY));
        Assertions.assertEquals(endpoint, storageEnv.getEnvs().get(S3Env.ENV_ENDPOINT));
        Assertions.assertEquals(bucket, storageEnv.getEnvs().get(S3Env.ENV_BUCKET));
        Assertions.assertEquals(region, storageEnv.getEnvs().get(S3Env.ENV_REGION));
        Assertions.assertEquals(prefixKey, storageEnv.getEnvs().get(S3Env.ENV_KEY_PREFIX));
    }

}
