/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import java.util.List;

public interface SWModelPackageVersionMapper {

    List<SWModelPackageVersionEntity> listVersions(Long swmpId, String namePrefix);

    SWModelPackageVersionEntity getLatestVersion(Long swmpId);

    int revertTo(Long swmpId, Long swmpVersionId);

    int addNewVersion(SWModelPackageVersionEntity entity);

    int update(SWModelPackageVersionEntity entity);
}
