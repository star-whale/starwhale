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
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessService;
import ai.starwhale.mlops.storage.autofit.CompatibleStorageAccessServiceBuilder;
import ai.starwhale.mlops.storage.autofit.StorageConnectionToken;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class StorageAccessParser implements SystemSettingListener {

    final StorageAccessService defaultStorageAccessService;

    final List<CompatibleStorageAccessServiceBuilder> compatibleStorageAccessServiceBuilders;

    /**
     * key: prefixWithoutPath
     * val: StorageAccessService
     */
    ConcurrentHashMap<String, StorageAccessService> storageAccessServiceCache = new ConcurrentHashMap<>();

    Map<StorageConnectionToken, CompatibleStorageAccessService> storageAccessServicesPool
            = new ConcurrentHashMap<>();

    public StorageAccessParser(StorageAccessService defaultStorageAccessService,
            List<CompatibleStorageAccessServiceBuilder> compatibleStorageAccessServiceBuilders) {
        this.defaultStorageAccessService = defaultStorageAccessService;
        this.compatibleStorageAccessServiceBuilders = compatibleStorageAccessServiceBuilders;
    }

    public StorageAccessService getStorageAccessServiceFromUri(StorageUri storageUri) {

        if (storageUri.getSchema() == null) {
            return defaultStorageAccessService;
        }
        StorageAccessService storageAccessService = storageAccessServiceCache.computeIfAbsent(
                storageUri.getPrefixWithBucket(), key -> {
                    for (CompatibleStorageAccessService css : storageAccessServicesPool.values()) {
                        if (css.compatibleWith(storageUri)) {
                            return css;
                        }
                    }
                    return null;
                });
        return storageAccessService == null ? defaultStorageAccessService : storageAccessService;

    }

    private Set<StorageConnectionToken> oldConnectionTokens = Set.of();

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
                        this::buildCompatibleStorageAccessService));
        oldConnectionTokens.stream().filter(t -> !connectionTokens.contains(t))
                .forEach(t -> {
                    CompatibleStorageAccessService removed = storageAccessServicesPool.remove(t);
                    Set<String> cachedPrefix = storageAccessServiceCache.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(removed)).map(
                                    Entry::getKey).collect(Collectors.toSet());
                    cachedPrefix.forEach(p -> {
                        storageAccessServiceCache.remove(p);
                    });
                });
        oldConnectionTokens = connectionTokens;
    }

    @Nullable
    private CompatibleStorageAccessService buildCompatibleStorageAccessService(StorageConnectionToken token) {
        for (CompatibleStorageAccessServiceBuilder builder : compatibleStorageAccessServiceBuilders) {
            if (builder.couldBuild(token.getType())) {
                return builder.build(token.getTokens());
            }
        }
        log.error("no builder  found for storage type {}", token.getType());
        return null;
    }
}
