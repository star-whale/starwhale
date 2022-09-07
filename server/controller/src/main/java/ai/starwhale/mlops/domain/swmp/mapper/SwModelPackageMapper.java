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

package ai.starwhale.mlops.domain.swmp.mapper;

import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SwModelPackageMapper {

    List<SwModelPackageEntity> listSwModelPackages(@Param("projectId") Long projectId,
            @Param("namePrefix") String namePrefix);
    //    List<SwModelPackageEntity> listSwModelPackagesByQuery(@Param("query")SwmpQuery query);

    int addSwModelPackage(@Param("entity") SwModelPackageEntity entity);

    int deleteSwModelPackage(@Param("id") Long id);

    int recoverSwModelPackage(@Param("id") Long id);

    SwModelPackageEntity findSwModelPackageById(@Param("id") Long id);

    List<SwModelPackageEntity> findSwModelPackagesByIds(@Param("ids") List<Long> ids);

    SwModelPackageEntity findByNameForUpdate(@Param("name") String name, @Param("projectId") Long projectId);

    SwModelPackageEntity findByName(@Param("name") String name, @Param("projectId") Long projectId);

    SwModelPackageEntity findDeletedSwModelPackageById(@Param("id") Long id);

    List<SwModelPackageEntity> listDeletedSwModelPackages(@Param("name") String name,
            @Param("projectId") Long projectId);

}
