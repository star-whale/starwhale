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

import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SwModelPackageVersionMapper {

    List<SwModelPackageVersionEntity> listVersions(@Param("swmpId") Long swmpId, @Param("namePrefix") String namePrefix,
            @Param("tag") String tag);

    SwModelPackageVersionEntity findVersionById(@Param("id") Long id);

    List<SwModelPackageVersionEntity> findVersionsByIds(@Param("dsVersionIds") List<Long> dsVersionIds);

    SwModelPackageVersionEntity getLatestVersion(@Param("swmpId") Long swmpId);

    int revertTo(@Param("swmpId") Long swmpId, @Param("swmpVersionId") Long swmpVersionId);

    int addNewVersion(@Param("version") SwModelPackageVersionEntity version);

    int update(@Param("version") SwModelPackageVersionEntity version);

    int updateTag(@Param("versionId") Long versionId, @Param("tag") String tag);

    SwModelPackageVersionEntity findByNameAndSwmpId(@Param("swmpVersion") String version, @Param("swmpId") Long id);

    SwModelPackageVersionEntity findByVersionOrderAndSwmpId(@Param("versionOrder") Long versionOrder,
            @Param("swmpId") Long id);
}
