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

package ai.starwhale.mlops.storage.autofit.s3;

import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.net.URISyntaxException;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompatibleStorageAccessServiceS3LikeTest {

    @Test
    public void testCompatibleWithoutEndpoint() throws URISyntaxException {
        CompatibleStorageAccessServiceS3Like compatibleStorageAccessServiceS3Like =
                new CompatibleStorageAccessServiceS3Like(mock(StorageAccessService.class),
                        S3Config.builder().accessKey("ak").secretKey("sk").bucket("b").region("r").build(),
                        Set.of("s3"));
        Assertions.assertTrue(compatibleStorageAccessServiceS3Like.compatibleWith(
                new StorageUri("s3://s3.r.amazonaws.com/b/adfa/d")));
        Assertions.assertFalse(compatibleStorageAccessServiceS3Like.compatibleWith(
                new StorageUri("s3://s3.r.amazonaws.com/c/adfa/d")));

    }

    @Test
    public void testCompatibleWithEndpoint() throws URISyntaxException {
        CompatibleStorageAccessServiceS3Like compatibleStorageAccessServiceS3Like =
                new CompatibleStorageAccessServiceS3Like(mock(StorageAccessService.class),
                        S3Config.builder().accessKey("ak").secretKey("sk").bucket("b").region("r")
                                .endpoint("http://localhost:9001").build(), Set.of("minio"));
        Assertions.assertTrue(
                compatibleStorageAccessServiceS3Like.compatibleWith(new StorageUri("minio://localhost:9001/b/adfa/d")));
        Assertions.assertFalse(
                compatibleStorageAccessServiceS3Like.compatibleWith(new StorageUri("minio://localhost:9001/c/adfa/d")));
    }

    @Test
    public void testCompatibleWithPortSame() throws URISyntaxException {
        CompatibleStorageAccessServiceS3Like compatibleStorageAccessServiceS3Like =
                new CompatibleStorageAccessServiceS3Like(mock(StorageAccessService.class),
                        S3Config.builder().accessKey("ak").secretKey("sk").bucket("b").region("r")
                                .endpoint("http://localhost:9001").build(), Set.of("minio"));
        Assertions.assertTrue(
                compatibleStorageAccessServiceS3Like.compatibleWith(new StorageUri("minio://localhost:9001/b/adfa/d")));
        Assertions.assertFalse(
                compatibleStorageAccessServiceS3Like.compatibleWith(
                        new StorageUri("minio://localhost2:9001/b/adfa/d")));
    }

}
