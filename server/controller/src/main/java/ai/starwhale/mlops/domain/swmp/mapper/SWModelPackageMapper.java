/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp.mapper;

import ai.starwhale.mlops.domain.swmp.SWModelPackageEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWModelPackageMapper {

    List<SWModelPackageEntity> listSWModelPackages(@Param("projectId")Long projectId, @Param("namePrefix")String namePrefix);

    int addSWModelPackage(SWModelPackageEntity entity);

    int deleteSWModelPackage(@Param("id")Long id);

    SWModelPackageEntity findSWModelPackageById(@Param("id")Long id);

    List<SWModelPackageEntity> findSWModelPackagesByIds(@Param("ids")List<Long> ids);

    SWModelPackageEntity findByNameForUpdate(@Param("name")String name);

    SWModelPackageEntity findByName(@Param("name")String name);

}
