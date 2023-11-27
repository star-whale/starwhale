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

import ai.starwhale.mlops.domain.system.SystemSetting;
import ai.starwhale.mlops.domain.system.SystemSettingListener;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageConnectionToken;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.domain.DomainAwareStorageAccessService;
import ai.starwhale.mlops.storage.fs.FsConfig;
import ai.starwhale.mlops.storage.s3.S3Config;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorageAccessParser implements SystemSettingListener {

    final StorageAccessService defaultStorageAccessService;

    /**
     * key: prefixWithoutPath
     * val: StorageAccessService
     */
    ConcurrentHashMap<String, StorageAccessService> storageAccessServiceCache = new ConcurrentHashMap<>();

    Map<StorageConnectionToken, StorageAccessService> storageAccessServicesPool = new ConcurrentHashMap<>();
    private Set<StorageConnectionToken> oldConnectionTokens = Set.of();

    public StorageAccessParser(StorageAccessService defaultStorageAccessService) {
        this.defaultStorageAccessService = defaultStorageAccessService;
    }

    public StorageAccessService getStorageAccessServiceFromUri(StorageUri storageUri) {
        if (storageUri.getSchema() == null) {
            return this.defaultStorageAccessService;
        }
        return storageAccessServiceCache.computeIfAbsent(
                storageUri.getPrefixWithBucket(), key -> {
                    for (StorageAccessService service : storageAccessServicesPool.values()) {
                        if (service.compatibleWith(storageUri)) {
                            return service;
                        }
                    }
                    return defaultStorageAccessService;
                });
    }

    @Override
    public void onUpdate(SystemSetting systemSetting) {
        Set<StorageConnectionToken> storageSetting = systemSetting.getStorageSetting();
        storageSetting = storageSetting == null ? Set.of() : storageSetting;
        Set<StorageConnectionToken> connectionTokens = Set.copyOf(storageSetting);
        if (oldConnectionTokens.equals(connectionTokens)) {
            return;
        }
        connectionTokens.stream().filter(t -> !oldConnectionTokens.contains(t))
                .forEach(t -> storageAccessServicesPool.computeIfAbsent(t,
                        this::buildStorageAccessService));
        oldConnectionTokens.stream().filter(t -> !connectionTokens.contains(t))
                .forEach(t -> {
                    StorageAccessService removed = storageAccessServicesPool.remove(t);
                    Set<String> cachedPrefix = storageAccessServiceCache.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(removed)).map(
                                    Entry::getKey).collect(Collectors.toSet());
                    cachedPrefix.forEach(p -> storageAccessServiceCache.remove(p));
                });
        oldConnectionTokens = connectionTokens;
    }

    @Nullable
    private StorageAccessService buildStorageAccessService(StorageConnectionToken token) {
        try {
            switch (token.getType().toLowerCase()) {
                case "memory":
                    return StorageAccessService.getMemoryStorageAccessService();
                case "fs":
                case "file":
                    return StorageAccessService.getFileStorageAccessService(
                            new FsConfig(token.getTokens().get("rootDir"), token.getTokens().get("serviceProvider")));
                default:
                    return new DomainAwareStorageAccessService(
                            StorageAccessService.getS3LikeStorageAccessService(
                                    token.getType(),
                                    new S3Config(token.getTokens())
                            )
                    );
            }
        } catch (Exception e) {
            log.error("can not build storage access service", e);
            return null;
        }
    }
}
