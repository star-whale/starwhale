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
import ai.starwhale.mlops.storage.s3.BotoS3Config;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class StorageEnvsPropertiesConverter {

    final StorageProperties storageProperties;

    static final String KEY_BUCKET = "USER.%s.%sBUCKET";
    static final String KEY_REGION = "USER.%s.%sREGION";
    static final String KEY_ENDPOINT = "USER.%s.%sENDPOINT";
    static final String KEY_SECRET = "USER.%s.%sSECRET";
    static final String KEY_ACCESS_KEY = "USER.%s.%sACCESS_KEY";

    public StorageEnvsPropertiesConverter(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public S3Config envToS3Config(StorageEnv env, StorageUri storageUri, String authName) {
        if (StringUtils.hasText(authName)) {
            authName = authName + ".";
        } else {
            authName = "";
        }
        authName = authName.toUpperCase();
        Map<String, String> envs = env.getEnvs();
        String bucket = null != storageUri && StringUtils.hasText(storageUri.getBucket()) ? storageUri.getBucket()
                : envs.get(String.format(KEY_BUCKET, env.getEnvType().name(), authName));
        String accessKey =
                null != storageUri && StringUtils.hasText(storageUri.getUsername()) ? storageUri.getUsername()
                        : envs.get(String.format(KEY_ACCESS_KEY, env.getEnvType().name(), authName));
        String accessSecret =
                null != storageUri && StringUtils.hasText(storageUri.getPassword()) ? storageUri.getPassword()
                        : envs.get(String.format(KEY_SECRET, env.getEnvType().name(), authName));
        String endpoint = null != storageUri && StringUtils.hasText(storageUri.getHost()) ? buildEndPoint(storageUri)
                : envs.get(String.format(KEY_ENDPOINT, env.getEnvType().name(), authName));
        return S3Config.builder()
                .bucket(bucket)
                .accessKey(accessKey)
                .secretKey(accessSecret)
                .region(envs.get(String.format(KEY_REGION, env.getEnvType().name(), authName)))
                .endpoint(endpoint)
                .build();
    }

    private String buildEndPoint(StorageUri storageUri) {
        if (null == storageUri.getPort() || 80 == storageUri.getPort()) {
            return "http://" + storageUri.getHost();
        } else if (443 == storageUri.getPort()) {
            return "https://" + storageUri.getHost();
        } else {
            return "http://" + storageUri.getHost() + ":" + storageUri.getPort();
        }

    }

    public Map<String, StorageEnv> propertiesToEnvs() {
        Map<String, StorageEnv> ret = new HashMap<>();
        if (null == storageProperties || storageProperties.getS3Config() == null) {
            return ret;
        }

        String t = storageProperties.getType();
        if (t == null || t.isEmpty()) {
            // make s3 as default type
            t = StorageEnvType.S3.name();
        }

        if (t.equalsIgnoreCase(StorageEnvType.S3.name()) || t.equalsIgnoreCase(
                StorageEnvType.MINIO.name())) {
            var s3Env = new S3Env();
            updateS3Env(s3Env, storageProperties);
            ret.put(s3Env.getEnvType().name(), s3Env);
        } else if (t.equalsIgnoreCase(StorageEnvType.ALIYUN.name())) {
            var aliyunEnv = new AliyunEnv();
            updateS3Env(aliyunEnv, storageProperties);
            // force using virtual host path
            aliyunEnv.setExtraS3Configs(new BotoS3Config(BotoS3Config.AddressingStyleType.VIRTUAL).toEnvStr());
            ret.put(aliyunEnv.getEnvType().name(), aliyunEnv);
        } else {
            log.error("storage type {} is not supported yet", t);
        }

        return ret;
    }

    private void updateS3Env(S3Env s3Env, StorageProperties storageProperties) {
        s3Env.setEndPoint(storageProperties.getS3Config().getEndpoint())
                .setBucket(storageProperties.getS3Config().getBucket())
                .setAccessKey(storageProperties.getS3Config().getAccessKey())
                .setSecret(storageProperties.getS3Config().getSecretKey())
                .setRegion(storageProperties.getS3Config().getRegion())
                .setKeyPrefix(storageProperties.getPathPrefix());
    }

}
