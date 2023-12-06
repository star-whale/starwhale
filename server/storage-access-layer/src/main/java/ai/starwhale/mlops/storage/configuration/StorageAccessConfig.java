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

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.domain.DomainAwareStorageAccessService;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAccessConfig {

    @Bean
    @ConfigurationProperties(prefix = "sw.storage")
    public StorageAccessService storageAccessService(StorageProperties storageProperties) {
        switch (storageProperties.getType().toLowerCase()) {
            case "memory":
                return new StorageAccessServiceMemory();
            case "fs":
            case "file":
                return new StorageAccessServiceFile(storageProperties.getFsConfig());
            default:
                return new DomainAwareStorageAccessService(
                        StorageAccessService.getS3LikeStorageAccessService(
                                storageProperties.getType(),
                                storageProperties.getS3Config()
                        )
                );
        }
    }
}
