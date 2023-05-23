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

package ai.starwhale.mlops.domain.model.bo;

import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelVersion {

    private Long id;

    private Long modelId;

    private String name;

    private Long ownerId;

    private String tag;

    private String jobs;

    private String storagePath;

    private String builtInRuntime;

    public static ModelVersion fromEntity(ModelVersionEntity entity) {
        return ModelVersion.builder()
                .id(entity.getId())
                .modelId(entity.getModelId())
                .name(entity.getName())
                .ownerId(entity.getOwnerId())
                .tag(entity.getVersionTag())
                .jobs(entity.getJobs())
                .builtInRuntime(entity.getBuiltInRuntime())
                .build();
    }
}
