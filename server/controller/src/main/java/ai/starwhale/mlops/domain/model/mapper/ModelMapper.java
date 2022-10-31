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

package ai.starwhale.mlops.domain.model.mapper;

import ai.starwhale.mlops.domain.model.po.ModelEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModelMapper {

    List<ModelEntity> listModels(@Param("projectId") Long projectId,
            @Param("namePrefix") String namePrefix);

    int addModel(@Param("entity") ModelEntity entity);

    int deleteModel(@Param("id") Long id);

    int recoverModel(@Param("id") Long id);

    ModelEntity findModelById(@Param("id") Long id);

    List<ModelEntity> findModelsByIds(@Param("ids") List<Long> ids);

    ModelEntity findByNameForUpdate(@Param("name") String name, @Param("projectId") Long projectId);

    ModelEntity findByName(@Param("name") String name, @Param("projectId") Long projectId);

    ModelEntity findDeletedModelById(@Param("id") Long id);

    List<ModelEntity> listDeletedModel(@Param("name") String name,
            @Param("projectId") Long projectId);

}
