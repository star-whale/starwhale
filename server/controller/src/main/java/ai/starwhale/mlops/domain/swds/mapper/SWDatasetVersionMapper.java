/**
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

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWDatasetVersionMapper {

    List<SWDatasetVersionEntity> listVersions(@Param("datasetId")Long datasetId,
        @Param("namePrefix")String namePrefix,
        @Param("tag")String tag);

    SWDatasetVersionEntity getVersionById(@Param("dsVersionId")Long dsVersionId);

    List<SWDatasetVersionEntity> findVersionsByIds(@Param("ids")List<Long> ids);

    List<SWDatasetVersionEntity> findVersionsByStatus(@Param("status")Integer status);

    SWDatasetVersionEntity getLatestVersion(@Param("datasetId")Long datasetId);

    SWDatasetVersionEntity findByDSIdAndVersionNameForUpdate(@Param("datasetId")Long datasetId,@Param("versionName")String versionName);

    int revertTo(@Param("dsId")Long dsId, @Param("dsVersionId")Long dsVersionId);

    int addNewVersion(@Param("version")SWDatasetVersionEntity version);

    int update(@Param("version")SWDatasetVersionEntity version);

    int updateFilesUploaded(@Param("version")SWDatasetVersionEntity version);

    int updateStatus(@Param("id")Long id ,@Param("status")Integer status);

    int deleteById(@Param("id")Long id);

}
