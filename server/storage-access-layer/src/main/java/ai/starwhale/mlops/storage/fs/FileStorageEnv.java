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

package ai.starwhale.mlops.storage.fs;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * holds storage connection information
 */
public class FileStorageEnv {

    @Getter
    Map<String, String> envs = new HashMap<>();

    @Getter
    FileSystemEnvType envType;

    public static final String ENV_TYPE="SW_STORAGE_ENV_TYPE";

    public static final String ENV_KEY_PREFIX="SW_OBJECT_STORE_KEY_PREFIX";

    public enum FileSystemEnvType {
        S3, HDFS, NFS, LOCAL_FS, REST_RESOURCE, FTP
    }

    public FileStorageEnv add(String name,String value){
        envs.put(name.toUpperCase(),value);
        return this;
    }

    public FileStorageEnv(FileSystemEnvType envType){
        this.envType = envType;
        envs.put(ENV_TYPE,envType.name());
    }

}
