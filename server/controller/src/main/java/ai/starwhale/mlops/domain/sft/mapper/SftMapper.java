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

package ai.starwhale.mlops.domain.sft.mapper;

import ai.starwhale.mlops.domain.sft.po.SftEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SftMapper {
    String COLUMNS = "id,           \n"
            + "space_id,      \n"
            + "job_id,       \n"
            + "eval_datasets,     \n"
            + "train_datasets,     \n"
            + "base_model_version_id,     \n"
            + "target_model_version_id,     \n"
            + "created_time, \n"
            + "modified_time ";

    @Insert("insert into sft"
            + " (space_id, job_id, eval_datasets, train_datasets, base_model_version_id, target_model_version_id)"
            + " values (#{spaceId}, #{jobId}, #{evalDatasets, typeHandler=ai.starwhale.mlops.domain.sft.mapper"
            + ".ListStringTypeHandler}"
            + ",#{trainDatasets, typeHandler=ai.starwhale.mlops.domain.sft.mapper.ListStringTypeHandler},"
            + " #{baseModelVersionId}, #{targetModelVersionId})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void add(SftEntity sftEntity);

    @Results({
            @Result(property = "evalDatasets", column = "eval_datasets", typeHandler = ListStringTypeHandler.class),
            @Result(property = "trainDatasets", column = "train_datasets", typeHandler = ListStringTypeHandler.class)
    })
    @Select("select " + COLUMNS + " from sft where space_id = #{spaceId} order by id desc")
    List<SftEntity> list(Long spaceId);

    @Results({
            @Result(property = "evalDatasets", column = "eval_datasets", typeHandler = ListStringTypeHandler.class),
            @Result(property = "trainDatasets", column = "train_datasets", typeHandler = ListStringTypeHandler.class)
    })
    @Select("select " + COLUMNS + " from sft where job_id = #{jobId}")
    SftEntity findSftByJob(Long jobId);

    @Update("update sft set target_model_version_id = #{targetModelVersionId} where id = #{sftId}")
    int updateTargetModel(Long sftId, Long targetModelVersionId);

    @Update("update sft set job_id = #{jobId} where id = #{sftId}")
    int updateJobId(Long sftId, Long jobId);
}
