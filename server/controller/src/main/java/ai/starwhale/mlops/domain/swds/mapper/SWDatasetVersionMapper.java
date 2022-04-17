/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.mapper;

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWDatasetVersionMapper {

    List<SWDatasetVersionEntity> listVersions(@Param("datasetId")Long datasetId, @Param("namePrefix")String namePrefix);

    SWDatasetVersionEntity getVersionById(@Param("dsVersionId")Long dsVersionId);

    List<SWDatasetVersionEntity> findVersionsByIds(@Param("ids")List<Long> ids);

    List<SWDatasetVersionEntity> findVersionsByStatus(@Param("status")Integer status);

    SWDatasetVersionEntity getLatestVersion(@Param("datasetId")Long datasetId);

    SWDatasetVersionEntity findByDSIdAndVersionNameForUpdate(@Param("datasetId")Long datasetId,@Param("versionName")String versionName);

    int revertTo(@Param("dsId")Long dsId, @Param("dsVersionId")Long dsVersionId);

    int addNewVersion(SWDatasetVersionEntity entity);

    int update(SWDatasetVersionEntity entity);

    int updateFilesUploaded(SWDatasetVersionEntity entity);

    int updateStatus(@Param("id")Long id ,@Param("status")Integer status);

    int deleteById(@Param("id")Long id);

}
