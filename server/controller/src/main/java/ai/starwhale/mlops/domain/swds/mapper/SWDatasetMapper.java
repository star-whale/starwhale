/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.mapper;

import ai.starwhale.mlops.domain.swds.SWDatasetEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWDatasetMapper {

    List<SWDatasetEntity> listDatasets(@Param("projectId") Long projectId, @Param("namePrefix")String namePrefix);

    int addDataset(SWDatasetEntity entity);

    int deleteDataset(@Param("id")Long id);

    SWDatasetEntity findDatasetById(@Param("id")Long id);

    List<SWDatasetEntity> findDatasetsByIds(@Param("ids")List<Long> ids);

    SWDatasetEntity findByName(@Param("name")String name);
}
