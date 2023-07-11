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

package ai.starwhale.mlops.domain.dataset.build.mapper;

import ai.starwhale.mlops.domain.dataset.build.BuildStatus;
import ai.starwhale.mlops.domain.dataset.build.po.BuildRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BuildRecordMapper {
    String COLUMNS_FOR_INSERT = "dataset_id, dataset_name, project_id, "
            + "type, status, storage_path, format, created_time";
    String COLUMNS_FOR_SELECT = "id, " + COLUMNS_FOR_INSERT;

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM dataset_build_record WHERE id = #{id}")
    BuildRecordEntity selectById(Long id);

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM dataset_build_record "
            + "WHERE project_id = #{projectId} AND status = #{status}")
    List<BuildRecordEntity> selectByStatus(@Param("projectId") Long projectId, @Param("status") BuildStatus status);

    @Insert("INSERT INTO dataset_build_record (" + COLUMNS_FOR_INSERT + ") "
            + "VALUES ("
            + "#{datasetId}, #{datasetName}, #{projectId}, "
            + "#{type}, #{status}, #{storagePath}, #{format}, #{createdTime}"
            + ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BuildRecordEntity buildRecord);
}
