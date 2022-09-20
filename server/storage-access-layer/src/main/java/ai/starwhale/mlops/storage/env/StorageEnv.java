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

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * holds storage connection information
 */
public class StorageEnv {

    @Getter
    Map<String, String> envs = new HashMap<>();

    @Getter
    StorageEnvType envType;

    public static final String ENV_TYPE = "SW_STORAGE_ENV_TYPE";

    public static final String ENV_KEY_PREFIX = "SW_OBJECT_STORE_KEY_PREFIX";

    public enum StorageEnvType {
        S3, MINIO, ALIYUN, HDFS, NFS, LOCAL_FS, REST_RESOURCE, FTP
    }

    public StorageEnv add(String name, String value) {
        envs.put(name.toUpperCase(), value);
        return this;
    }

    public StorageEnv(StorageEnvType envType) {
        this.envType = envType;
        envs.put(ENV_TYPE, envType.name());
    }

    public void setKeyPrefix(String keyPrefix) {
        this.add(ENV_KEY_PREFIX, keyPrefix);
    }
}
