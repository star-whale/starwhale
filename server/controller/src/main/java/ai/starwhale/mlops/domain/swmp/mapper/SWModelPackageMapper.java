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
