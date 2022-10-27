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

package ai.starwhale.mlops.domain.dataset.mapper;

import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DatasetVersionMapper {

    List<DatasetVersionEntity> listVersions(@Param("datasetId") Long datasetId,
            @Param("namePrefix") String namePrefix,
            @Param("tag") String tag);

    DatasetVersionEntity getVersionById(@Param("dsVersionId") Long dsVersionId);

    List<DatasetVersionEntity> findVersionsByIds(@Param("ids") List<Long> ids);

    List<DatasetVersionEntity> findVersionsByNames(@Param("names") List<String> names);

    List<DatasetVersionEntity> findVersionsByStatus(@Param("status") Integer status);

    DatasetVersionEntity getLatestVersion(@Param("datasetId") Long datasetId);

    DatasetVersionEntity findByDsIdAndVersionNameForUpdate(@Param("datasetId") Long datasetId,
            @Param("versionName") String versionName);

    DatasetVersionEntity findByDsIdAndVersionName(@Param("datasetId") Long datasetId,
            @Param("versionName") String versionName);

    DatasetVersionEntity findByDsIdAndVersionOrder(@Param("datasetId") Long datasetId,
            @Param("versionOrder") Long versionOrder);

    int revertTo(@Param("dsId") Long dsId, @Param("dsVersionId") Long dsVersionId);

    int addNewVersion(@Param("version") DatasetVersionEntity version);

    int update(@Param("version") DatasetVersionEntity version);

    int updateTag(@Param("versionId") Long versionId, @Param("tag") String tag);

    int updateFilesUploaded(@Param("version") DatasetVersionEntity version);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateStorageAuths(@Param("id") Long id, @Param("storageAuths") String storageAuths);

    int deleteById(@Param("id") Long id);

}
