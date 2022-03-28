/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWDatasetVersionMapper {

    List<SWDatasetVersionEntity> listVersions(@Param("datasetId")Long datasetId, @Param("namePrefix")String namePrefix);

    SWDatasetVersionEntity getLatestVersion(@Param("datasetId")Long datasetId);

    int revertTo(@Param("dsId")Long dsId, @Param("dsVersionId")Long dsVersionId);

    int addNewVersion(SWDatasetVersionEntity entity);

    int update(SWDatasetVersionEntity entity);

}
