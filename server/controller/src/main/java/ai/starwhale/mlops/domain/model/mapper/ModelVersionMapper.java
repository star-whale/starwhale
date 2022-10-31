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

import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModelVersionMapper {

    List<ModelVersionEntity> listVersions(@Param("modelId") Long modelId, @Param("namePrefix") String namePrefix,
            @Param("tag") String tag);

    ModelVersionEntity findVersionById(@Param("id") Long id);

    List<ModelVersionEntity> findVersionsByIds(@Param("dsVersionIds") List<Long> dsVersionIds);

    ModelVersionEntity getLatestVersion(@Param("modelId") Long modelId);

    int revertTo(@Param("modelId") Long modelId, @Param("modelVersionId") Long modelVersionId);

    int addNewVersion(@Param("version") ModelVersionEntity version);

    int update(@Param("version") ModelVersionEntity version);

    int updateTag(@Param("versionId") Long versionId, @Param("tag") String tag);

    ModelVersionEntity findByNameAndModelId(@Param("modelVersion") String version, @Param("modelId") Long id);

    ModelVersionEntity findByVersionOrderAndModelId(@Param("versionOrder") Long versionOrder,
            @Param("modelId") Long id);
}
