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

package ai.starwhale.mlops.domain.runtime.po;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeVersionEntity extends BaseEntity implements BundleVersionEntity {

    private Long id;

    private Long versionOrder;

    private Long runtimeId;

    private Long ownerId;

    private String versionName;

    private String versionTag;

    private String versionMeta;

    private String storagePath;

    private String image;

    private String builtImage;

    private Boolean shared;

    @Override
    public String getName() {
        return versionName;
    }
}
