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

package ai.starwhale.mlops.domain.swds.converter;

import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SWDSBOConverter {

    final StorageProperties storageProperties;

    public SWDSBOConverter(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public SWDataSet fromEntity(SWDatasetVersionEntity swDatasetVersionEntity){
        Map<String, FileStorageEnv> fileStorageEnvs = storageProperties.toFileStorageEnvs();
        fileStorageEnvs.values().forEach(fileStorageEnv -> fileStorageEnv.add(FileStorageEnv.ENV_KEY_PREFIX,swDatasetVersionEntity.getStoragePath()));
        return SWDataSet.builder()
            .id(swDatasetVersionEntity.getId())
            .name(swDatasetVersionEntity.getDatasetName())
            .version(swDatasetVersionEntity.getVersionName())
            .size(swDatasetVersionEntity.getSize())
            .path(swDatasetVersionEntity.getStoragePath())
            .fileStorageEnvs(fileStorageEnvs)
            .indexTable(swDatasetVersionEntity.getIndexTable())
            .build();
    }
}
