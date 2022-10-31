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

package ai.starwhale.mlops.domain.dataset.converter;

import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.storage.env.StorageEnv;
import ai.starwhale.mlops.storage.env.StorageEnvsPropertiesConverter;
import ai.starwhale.mlops.storage.env.UserStorageAuthEnv;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatasetBoConverter {

    final StorageEnvsPropertiesConverter storageEnvsPropertiesConverter;

    public DatasetBoConverter(StorageEnvsPropertiesConverter storageEnvsPropertiesConverter) {
        this.storageEnvsPropertiesConverter = storageEnvsPropertiesConverter;
    }

    public DataSet fromEntity(DatasetVersionEntity datasetVersionEntity) {
        Map<String, StorageEnv> fileStorageEnvs;
        if (StringUtils.hasText(datasetVersionEntity.getStorageAuths())) {
            UserStorageAuthEnv storageAuths = new UserStorageAuthEnv(datasetVersionEntity.getStorageAuths());
            fileStorageEnvs = storageAuths.allEnvs();
        } else {
            fileStorageEnvs = storageEnvsPropertiesConverter.propertiesToEnvs();
        }
        fileStorageEnvs.values().forEach(fileStorageEnv -> fileStorageEnv.add(StorageEnv.ENV_KEY_PREFIX,
                datasetVersionEntity.getStoragePath()));
        return DataSet.builder()
                .id(datasetVersionEntity.getId())
                .name(datasetVersionEntity.getDatasetName())
                .version(datasetVersionEntity.getVersionName())
                .size(datasetVersionEntity.getSize())
                .path(datasetVersionEntity.getStoragePath())
                .fileStorageEnvs(fileStorageEnvs)
                .indexTable(datasetVersionEntity.getIndexTable())
                .build();
    }
}
