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

package ai.starwhale.mlops.domain.job.step.mapper;

import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StepMapper {

    @Insert("insert into step(step_uuid, step_name, job_id, last_step_id, step_status, concurrency, task_num,"
            + " pool_info, origin_json)"
            + " values (#{step.uuid}, #{step.name}, #{step.jobId}, #{step.lastStepId}, #{step.status}, "
            + " #{step.concurrency}, #{step.taskNum}, #{step.poolInfo}, #{step.originJson})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int save(@Param("step") StepEntity stepEntity);

    @Select("select id, step_uuid as uuid, step_name as name, job_id, last_step_id, step_status as status,"
            + " finished_time, started_time, created_time, modified_time, concurrency, task_num, pool_info,"
            + " origin_json from step"
            + " where job_id = #{jobId}"
            + " order by id")
    List<StepEntity> findByJobId(@Param("jobId") Long jobId);

    @Select("select id, step_uuid as uuid, step_name as name, job_id, last_step_id, step_status as status,"
            + " finished_time, started_time, created_time, modified_time, concurrency, task_num, pool_info,"
            + " origin_json from step"
            + " where id = #{id}")
    StepEntity findById(@Param("id") Long id);

    @Update("update step set last_step_id = #{lastStepId} WHERE id = #{stepId}")
    void updateLastStep(@Param("stepId") Long stepId, @Param("lastStepId") Long lastStepId);

    @Update("<script>"
            + "update step set step_status = #{status} WHERE id in "
            + " <foreach item='item' index='index' collection='stepIds'"
            + "   open='(' separator=',' close=')'>"
            + "    #{item}"
            + " </foreach>"
            + "</script>")
    void updateStatus(@Param("stepIds") List<Long> stepIds, @Param("status") StepStatus status);

    @Update("update step set finished_time = #{finishedTime} WHERE id = #{stepId}")
    void updateFinishedTime(@Param("stepId") Long stepId, @Param("finishedTime") Date finishedTime);

    @Update("update step set started_time = #{startedTime} WHERE id = #{stepId}")
    void updateStartedTime(@Param("stepId") Long stepId, @Param("startedTime") Date startedTime);
}
