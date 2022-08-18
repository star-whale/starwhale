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

package ai.starwhale.mlops.storage.configuration;

import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import ai.starwhale.mlops.storage.fs.S3Env;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.storage")
public class StorageProperties {
    String type;
    String pathPrefix;
    S3Config s3Config;

    public Map<String, FileStorageEnv> toFileStorageEnvs(){
        Map<String, FileStorageEnv> ret = new HashMap<>();
        if(null != s3Config){
            S3Env s3Env = new S3Env();
            s3Env.add(S3Env.ENV_BUCKET,s3Config.getBucket());
            s3Env.add(S3Env.ENV_ENDPOINT,s3Config.getEndpoint());
            s3Env.add(S3Env.ENV_SECRET_ID,s3Config.getAccessKey());
            s3Env.add(S3Env.ENV_SECRET_KEY,s3Config.getSecretKey());
            s3Env.add(S3Env.ENV_REGION,s3Config.getRegion());
            s3Env.add(S3Env.ENV_KEY_PREFIX,pathPrefix);
            ret.put(s3Env.getEnvType().name(),s3Env);
        }
        return ret;
    }
}
