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

package ai.starwhale.mlops.domain.dataset.objectstore;

import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.env.StorageEnv;
import ai.starwhale.mlops.storage.env.UserStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.env.UserStorageAuthEnv;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StorageAccessParser {

    final StorageAccessService defaultStorageAccessService;

    final DatasetVersionMapper datasetVersionMapper;

    final UserStorageAccessServiceBuilder userStorageAccessServiceBuilder;

    ConcurrentHashMap<String, StorageAccessService> storageAccessServicePool = new ConcurrentHashMap<>();

    public StorageAccessParser(StorageAccessService defaultStorageAccessService,
            DatasetVersionMapper datasetVersionMapper,
            UserStorageAccessServiceBuilder userStorageAccessServiceBuilder) {
        this.defaultStorageAccessService = defaultStorageAccessService;
        this.datasetVersionMapper = datasetVersionMapper;
        this.userStorageAccessServiceBuilder = userStorageAccessServiceBuilder;
    }

    public StorageAccessService getStorageAccessServiceFromAuth(Long datasetId, String uri,
            String authName) {
        if (StringUtils.hasText(authName)) {
            authName = authName.toUpperCase(); // env vars are uppercase always
        } else {
            return defaultStorageAccessService;
        }
        StorageAccessService cachedStorageAccessService = storageAccessServicePool.get(
                formatKey(datasetId, authName));
        if (null != cachedStorageAccessService) {
            return cachedStorageAccessService;
        }
        DatasetVersionEntity datasetVersionEntity = datasetVersionMapper.getVersionById(
                datasetId);
        String storageAuthsText = datasetVersionEntity.getStorageAuths();
        if (!StringUtils.hasText(storageAuthsText)) {
            return defaultStorageAccessService;
        }

        UserStorageAuthEnv storageAuths = new UserStorageAuthEnv(storageAuthsText);
        StorageEnv env = storageAuths.getEnv(authName);
        if (null == env) {
            return defaultStorageAccessService;
        }

        StorageAccessService storageAccessService = userStorageAccessServiceBuilder.build(env, new StorageUri(uri),
                authName);
        if (null == storageAccessService) {
            throw new SwValidationException(ValidSubject.DATASET, "file system not supported yet: " + env.getEnvType());
        }
        storageAccessServicePool.putIfAbsent(formatKey(datasetId, authName), storageAccessService);
        return storageAccessService;
    }

    String formatKey(Long datasetId, String authName) {
        return datasetId.toString() + authName;
    }

}
