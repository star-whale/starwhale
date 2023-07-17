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
import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.autofit.aliyun.CompatibleStorageAccessServiceBuilderAliyun;
import ai.starwhale.mlops.storage.autofit.fs.CompatibleStorageAccessServiceBuilderFs;
import ai.starwhale.mlops.storage.autofit.minio.CompatibleStorageAccessServiceBuilderMinio;
import ai.starwhale.mlops.storage.autofit.qcloud.CompatibleStorageAccessServiceBuilderQcloud;
import ai.starwhale.mlops.storage.autofit.s3.CompatibleStorageAccessServiceBuilderS3;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import ai.starwhale.mlops.storage.qcloud.StorageAccessServiceQcloud;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAccessConfig {

    @Bean
    public CompatibleStorageAccessServiceBuilderFs compatibleStorageAccessServiceBuilderFs() {
        return new CompatibleStorageAccessServiceBuilderFs();
    }

    @Bean
    public CompatibleStorageAccessServiceBuilderAliyun compatibleStorageAccessServiceBuilderAliyun() {
        return new CompatibleStorageAccessServiceBuilderAliyun();
    }

    @Bean
    public CompatibleStorageAccessServiceBuilderQcloud compatibleStorageAccessServiceBuilderQcloud() {
        return new CompatibleStorageAccessServiceBuilderQcloud();
    }

    @Bean
    public CompatibleStorageAccessServiceBuilderMinio compatibleStorageAccessServiceBuilderMinio() {
        return new CompatibleStorageAccessServiceBuilderMinio();
    }

    @Bean
    public CompatibleStorageAccessServiceBuilderS3 compatibleStorageAccessServiceBuilderS3() {
        return new CompatibleStorageAccessServiceBuilderS3();
    }

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "fs")
    public StorageAccessService fs(StorageProperties storageProperties) {
        return new StorageAccessServiceFile(storageProperties.getFsConfig());
    }

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "s3")
    public StorageAccessService s3(StorageProperties storageProperties) {
        return new StorageAccessServiceS3(storageProperties.getS3Config());
    }

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "aliyun")
    public StorageAccessService aliyun(StorageProperties storageProperties) {
        return new StorageAccessServiceAliyun(storageProperties.getS3Config());
    }

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "tencent")
    public StorageAccessService qcloud(StorageProperties storageProperties) {
        return new StorageAccessServiceQcloud(storageProperties.getS3Config());
    }

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "minio", matchIfMissing = true)
    public StorageAccessService minio(StorageProperties storageProperties) {
        return new StorageAccessServiceMinio(storageProperties.getS3Config());
    }

    @Bean
    @ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "memory")
    public StorageAccessService memory(StorageProperties storageProperties) {
        return new StorageAccessServiceMemory();
    }
}
