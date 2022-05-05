/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp.mapper;

import ai.starwhale.mlops.domain.swmp.SWModelPackageVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SWModelPackageVersionMapper {

    List<SWModelPackageVersionEntity> listVersions(@Param("swmpId") Long swmpId, @Param("namePrefix")String namePrefix);

    SWModelPackageVersionEntity findVersionById(@Param("id")Long id);
    List<SWModelPackageVersionEntity> findVersionsByIds(@Param("dsVersionIds")List<Long> dsVersionIds);

    SWModelPackageVersionEntity getLatestVersion(@Param("swmpId")Long swmpId);

    int revertTo(@Param("swmpVersionId")Long swmpVersionId);

    int addNewVersion(SWModelPackageVersionEntity entity);

    int update(SWModelPackageVersionEntity entity);

    SWModelPackageVersionEntity findByNameAndSwmpId(@Param("swmpVersion")String version, @Param("swmpId")Long id);
}
