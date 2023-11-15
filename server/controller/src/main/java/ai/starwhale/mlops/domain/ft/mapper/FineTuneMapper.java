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

package ai.starwhale.mlops.domain.ft.mapper;

import ai.starwhale.mlops.domain.ft.po.FineTuneEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FineTuneMapper {
    String COLUMNS = "id,           \n"
            + "space_id,      \n"
            + "job_id,       \n"
            + "eval_datasets,     \n"
            + "train_datasets,     \n"
            + "base_model_version_id,     \n"
            + "target_model_version_id,     \n"
            + "created_time, \n"
            + "modified_time ";

    @Insert("insert into fine_tune"
            + " (space_id, job_id, eval_datasets, train_datasets, base_model_version_id, target_model_version_id)"
            + " values (#{spaceId}, #{jobId}, #{evalDatasets, typeHandler=ai.starwhale.mlops.domain.ft.mapper"
            + ".ListStringTypeHandler}"
            + ",#{trainDatasets, typeHandler=ai.starwhale.mlops.domain.ft.mapper.ListStringTypeHandler},"
            + " #{baseModelVersionId}, #{targetModelVersionId})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void add(FineTuneEntity fineTuneEntity);

    @Results({
            @Result(property = "evalDatasets", column = "eval_datasets", typeHandler = ListStringTypeHandler.class),
            @Result(property = "trainDatasets", column = "train_datasets", typeHandler = ListStringTypeHandler.class)
    })
    @Select("select " + COLUMNS + " from fine_tune where space_id = #{spaceId} order by id desc")
    List<FineTuneEntity> list(Long spaceId);

    @Results({
            @Result(property = "evalDatasets", column = "eval_datasets", typeHandler = ListStringTypeHandler.class),
            @Result(property = "trainDatasets", column = "train_datasets", typeHandler = ListStringTypeHandler.class)
    })
    @Select("select " + COLUMNS + " from fine_tune where job_id = #{jobId}")
    FineTuneEntity findByJob(Long jobId);

    @Results({
            @Result(property = "evalDatasets", column = "eval_datasets", typeHandler = ListStringTypeHandler.class),
            @Result(property = "trainDatasets", column = "train_datasets", typeHandler = ListStringTypeHandler.class)
    })
    @Select("select " + COLUMNS + " from fine_tune where id = #{id}")
    FineTuneEntity findById(Long id);

    @Update("update fine_tune set target_model_version_id = #{targetModelVersionId} where id = #{id}")
    int updateTargetModel(Long id, Long targetModelVersionId);

    @Update("update fine_tune set job_id = #{jobId} where id = #{id}")
    int updateJobId(Long id, Long jobId);
}
