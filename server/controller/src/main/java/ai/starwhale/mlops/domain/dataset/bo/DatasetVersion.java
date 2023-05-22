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

package ai.starwhale.mlops.domain.dataset.bo;

import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatasetVersion {

    public static final int STATUS_AVAILABLE = 1;
    public static final int STATUS_UN_AVAILABLE = 0;
    private Long id;
    private Long datasetId;
    private Long versionOrder;
    private String datasetName;
    private Long ownerId;
    private String versionName;
    private String versionTag;
    private String versionMeta;
    private String filesUploaded;
    private String storagePath;
    private Long size;
    private String indexTable;
    /**
     * 0 - unavailable 1 - available
     */
    private Integer status = STATUS_UN_AVAILABLE;

    public static DatasetVersion fromEntity(DatasetEntity datasetEntity, DatasetVersionEntity versionEntity) {
        return DatasetVersion.builder()
                .id(versionEntity.getId())
                .datasetId(versionEntity.getDatasetId())
                .versionOrder(versionEntity.getVersionOrder())
                .datasetName(datasetEntity.getDatasetName())
                .ownerId(versionEntity.getOwnerId())
                .versionName(versionEntity.getVersionName())
                .versionTag(versionEntity.getVersionTag())
                .versionMeta(versionEntity.getVersionMeta())
                .filesUploaded(versionEntity.getFilesUploaded())
                .storagePath(versionEntity.getStoragePath())
                .size(versionEntity.getSize())
                .status(versionEntity.getStatus())
                .indexTable(versionEntity.getIndexTable())
                .build();
    }
}
