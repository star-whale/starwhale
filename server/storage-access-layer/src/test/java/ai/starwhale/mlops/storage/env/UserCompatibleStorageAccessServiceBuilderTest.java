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

import ai.starwhale.mlops.storage.env.StorageEnv.StorageEnvType;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import com.aliyun.oss.common.auth.InvalidCredentialsException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

public class UserCompatibleStorageAccessServiceBuilderTest {

    private UserStorageAccessServiceBuilder userStorageAccessServiceBuilder;

    @BeforeEach
    public void setUp() {
        userStorageAccessServiceBuilder = new UserStorageAccessServiceBuilder(new StorageEnvsPropertiesConverter(null));
    }

    @Test
    public void testAliyun() {
        Assertions.assertThrows(InvalidCredentialsException.class,
                () -> userStorageAccessServiceBuilder.build(new StorageEnv(
                        StorageEnvType.ALIYUN)
                        .add("USER.ALIYUN.ENDPOINT", "endpoint")
                        .add("USER.ALIYUN.ENDPOINT", "EDP")
                        .add("USER.ALIYUN.endpoint", "dpd")
                        .add("USER.S3.SECRET", "SCret")
                        .add("USER.ALIYUN.ACCESS_KEY", "ack")
                        .add("USER.ALIYUN.BUCKET", "bkt")
                        .add("USER.ALIYUN.REGION", "region"), null, null));
    }

    @Test
    public void testS3() {
        Assertions.assertThrows(SdkClientException.class, () -> userStorageAccessServiceBuilder.build(
                new StorageEnv(
                        StorageEnvType.S3)
                        .add("USER.S3.URTEST.ENDPOINT", "http://abcd.com")
                        .add("USER.S5.URTEST.ENDPOINT", "http://abcd.com/d")
                        .add("USER.S4.URTEST.endpoint", "dpd")
                        .add("USER.S3.URTEST.SECRET", "SCret")
                        .add("USER.S3.URTEST.ACCESS_KEY", "ack")
                        .add("USER.S3.URTEST.BUCKET", "bkt")
                        .add("USER.S3.URTEST.REGION", "region"), null,
                "URTEST"));
    }

    @Test
    public void testMinio() {
        Assertions.assertEquals(StorageAccessServiceMinio.class,
                userStorageAccessServiceBuilder.build(new StorageEnv(
                        StorageEnvType.MINIO)
                        .add("USER.MINIO.MYTEST.ENDPOINT", "endpoint")
                        .add("USER.MINIO.URTEST.ENDPOINT", "EDP")
                        .add("USER.S3.MYTEST.endpoint", "dpd")
                        .add("USER.MINIO.MYTEST.SECRET", "SCret")
                        .add("USER.MINIO.MYTEST.ACCESS_KEY", "ack")
                        .add("USER.MINIO.MYTEST.BUCKET", "bkt")
                        .add("USER.MINIO.MYTEST.REGION", "region"), null, "MYTEST").getClass());
    }

    @Test
    public void testNotSupported() {
        Assertions.assertNull(userStorageAccessServiceBuilder.build(new StorageEnv(
                StorageEnvType.FTP), null, null));
    }

}
