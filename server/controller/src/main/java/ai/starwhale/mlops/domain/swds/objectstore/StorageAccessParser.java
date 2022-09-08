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

import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import ai.starwhale.mlops.storage.fs.FileStorageEnv.FileSystemEnvType;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StorageAccessParser {

    final StorageAccessService defaultStorageAccessService;

    final SwDatasetVersionMapper swDatasetVersionMapper;

    ConcurrentHashMap<String, StorageAccessService> storageAccessServicePool = new ConcurrentHashMap<>();

    public StorageAccessParser(StorageAccessService defaultStorageAccessService,
            SwDatasetVersionMapper swDatasetVersionMapper) {
        this.defaultStorageAccessService = defaultStorageAccessService;
        this.swDatasetVersionMapper = swDatasetVersionMapper;
    }

    public StorageAccessService getStorageAccessServiceFromAuth(Long datasetId, String uri,
            String authName) {
        if (StringUtils.hasText(authName)) {
            authName = authName.toUpperCase(); // env vars are uppercase always
        }
        StorageAccessService cachedStorageAccessService = storageAccessServicePool.get(
                formatKey(datasetId, authName));
        if (null != cachedStorageAccessService) {
            return cachedStorageAccessService;
        }
        SwDatasetVersionEntity swDatasetVersionEntity = swDatasetVersionMapper.getVersionById(
                datasetId);
        String storageAuthsText = swDatasetVersionEntity.getStorageAuths();
        if (!StringUtils.hasText(storageAuthsText)) {
            return defaultStorageAccessService;
        }

        StorageAuths storageAuths = new StorageAuths(storageAuthsText);
        FileStorageEnv env = storageAuths.getEnv(authName);
        if (null == env) {
            return defaultStorageAccessService;
        }
        if (env.getEnvType() != FileSystemEnvType.S3) {
            throw new SwValidationException(ValidSubject.SWDS).tip(
                    "file system not supported yet: " + env.getEnvType());
        }
        StorageAccessServiceS3 storageAccessServiceS3 = new StorageAccessServiceS3(
                env2S3Config(new StorageUri(uri), env, authName));
        storageAccessServicePool.putIfAbsent(formatKey(datasetId, authName),
                storageAccessServiceS3);
        return storageAccessServiceS3;
    }

    String formatKey(Long datasetId, String authName) {
        return datasetId.toString() + authName;
    }


    static final String KEY_BUCKET = "USER.S3.%sBUCKET";
    static final String KEY_REGION = "USER.S3.%sREGION";
    static final String KEY_ENDPOINT = "USER.S3.%sENDPOINT";
    static final String KEY_SECRET = "USER.S3.%sSECRET";
    static final String KEY_ACCESS_KEY = "USER.S3.%sACCESS_KEY";

    S3Config env2S3Config(StorageUri storageUri, FileStorageEnv env, String authName) {
        if (StringUtils.hasText(authName)) {
            authName = authName + ".";
        } else {
            authName = "";
        }
        authName = authName.toUpperCase();
        Map<String, String> envs = env.getEnvs();
        String bucket = StringUtils.hasText(storageUri.getBucket()) ? storageUri.getBucket()
                : envs.get(String.format(KEY_BUCKET, authName));
        String accessKey = StringUtils.hasText(storageUri.getUsername()) ? storageUri.getUsername()
                : envs.get(String.format(KEY_ACCESS_KEY, authName));
        String accessSecret =
                StringUtils.hasText(storageUri.getPassword()) ? storageUri.getPassword()
                        : envs.get(String.format(KEY_SECRET, authName));
        String endpoint = StringUtils.hasText(storageUri.getHost()) ? buildEndPoint(storageUri)
                : envs.get(String.format(KEY_ENDPOINT, authName));
        return new S3Config(bucket, accessKey, accessSecret, envs.get(String.format(KEY_REGION, authName)), endpoint);
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

}
