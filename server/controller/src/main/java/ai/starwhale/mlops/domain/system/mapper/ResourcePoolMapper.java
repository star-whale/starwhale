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

package ai.starwhale.mlops.domain.system.mapper;

import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotNull;

import java.util.List;

public interface ResourcePoolMapper {

    List<ResourcePoolEntity> listResourcePools();

    Long add(@Param("resourcePoolEntity") ResourcePoolEntity resourcePoolEntity);

    void deleteById(@Param("id") Long id);

    void update(@Param("resourcePoolEntities") List<ResourcePoolEntity> resourcePoolEntities);

    ResourcePoolEntity findByLabel(@NotNull @Param("label") String label);

    ResourcePoolEntity findById(@NotNull @Param("id") Long id);
}
