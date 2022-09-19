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

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserStorageAccessServiceBuilder {

    final StorageEnvsPropertiesConverter storageEnvsPropertiesConverter;

    public UserStorageAccessServiceBuilder(
            StorageEnvsPropertiesConverter storageEnvsPropertiesConverter) {
        this.storageEnvsPropertiesConverter = storageEnvsPropertiesConverter;
    }

    @Nullable
    public StorageAccessService build(StorageEnv userEnvs, StorageUri storageUri, String authName) {
        switch (userEnvs.getEnvType()) {
            case S3:
                return new StorageAccessServiceS3(
                        storageEnvsPropertiesConverter.envToS3Config(userEnvs, storageUri, authName));
            case ALIYUN:
                return new StorageAccessServiceAliyun(
                        storageEnvsPropertiesConverter.envToS3Config(userEnvs, storageUri, authName));
            case MINIO:
                return new StorageAccessServiceMinio(
                        storageEnvsPropertiesConverter.envToS3Config(userEnvs, storageUri, authName));
            default:
                log.warn("storage type {} not supported yet", userEnvs.getEnvType());
                return null;
        }
    }

}
