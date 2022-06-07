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

package ai.starwhale.mlops.domain.swds.mapper;

import ai.starwhale.mlops.domain.swds.SWDatasetEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWDatasetMapper {

    List<SWDatasetEntity> listDatasets(@Param("projectId") Long projectId, @Param("namePrefix")String namePrefix);

    int addDataset(@Param("swds")SWDatasetEntity swds);

    int deleteDataset(@Param("id")Long id);

    int recoverDataset(@Param("id")Long id);
    SWDatasetEntity findDatasetById(@Param("id")Long id);

    List<SWDatasetEntity> findDatasetsByIds(@Param("ids")List<Long> ids);

    SWDatasetEntity findByNameForUpdate(@Param("name")String name);

    SWDatasetEntity findByName(@Param("name")String name);

    SWDatasetEntity findDeletedDatasetById(@Param("id")Long id);

    List<SWDatasetEntity> listDeletedDatasets(@Param("name")String name);
}
