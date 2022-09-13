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

import ai.starwhale.mlops.storage.fs.AliyunEnv;
import ai.starwhale.mlops.storage.fs.BotoS3Config;
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

    public Map<String, FileStorageEnv> toFileStorageEnvs() {
        Map<String, FileStorageEnv> ret = new HashMap<>();
        if (s3Config == null) {
            return ret;
        }

        String t = type;
        if (t.isEmpty()) {
            t = FileStorageEnv.FileSystemEnvType.S3.name();
        }
        if (t.equalsIgnoreCase(FileStorageEnv.FileSystemEnvType.S3.name())) {
            var s3Env = new S3Env();
            updateS3Config(s3Env);
            ret.put(s3Env.getEnvType().name(), s3Env);
        } else if (t.equalsIgnoreCase(FileStorageEnv.FileSystemEnvType.ALIYUN.name())) {
            var aliyunEnv = new AliyunEnv();
            updateS3Config(aliyunEnv);
            // force using virtual host path
            aliyunEnv.setExtraS3Configs(new BotoS3Config(BotoS3Config.AddressingStyleType.VIRTUAL).toEnvStr());
            ret.put(aliyunEnv.getEnvType().name(), aliyunEnv);
        }

        return ret;
    }

    private void updateS3Config(S3Env s3Env) {
        s3Env.setEndPoint(s3Config.getEndpoint())
                .setBucket(s3Config.getBucket())
                .setAccessKey(s3Config.getAccessKey())
                .setSecret(s3Config.getSecretKey())
                .setRegion(s3Config.getRegion())
                .setKeyPrefix(pathPrefix);
    }
}
