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

package ai.starwhale.mlops.domain.task.mapper;

import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper {

    String COLUMNS = "task_info.id, task_uuid, step_id, agent_id, task_status, task_request,"
            + " task_info.finished_time, task_info.started_time, task_info.created_time, task_info.modified_time,"
            + " retry_num, output_path";

    @Select("select " + COLUMNS + " from task_info"
            + " left join step s on s.id = task_info.step_id where s.job_id = #{jobId} order by id desc")
    List<TaskEntity> listTasks(@Param("jobId") Long jobId);

    @Select("select " + COLUMNS + " from task_info where id = #{taskId}")
    TaskEntity findTaskById(@Param("taskId") Long taskId);


    @Insert("insert into task_info(task_uuid, step_id, task_status, task_request, output_path)"
            + " values (#{task.taskUuid}, #{task.stepId}, #{task.taskStatus}, #{task.taskRequest}, #{task.outputPath})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int addTask(@Param("task") TaskEntity task);

    @Insert("<script>"
            + "insert into task_info(task_uuid, step_id, task_status, task_request, output_path) values"
            + "<foreach collection='taskList' item='task' index='index' open='(' separator='),(' close=')'>"
            + "  #{task.taskUuid}, #{task.stepId}, #{task.taskStatus}, #{task.taskRequest}, #{task.outputPath}"
            + "</foreach>"
            + "</script>")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int addAll(@Param("taskList") List<TaskEntity> taskList);

    @Update("<script>"
            + " update task_info set task_status = #{taskStatus} where id in"
            + " <foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>"
            + "   #{item}"
            + " </foreach>"
            + "</script>")
    void updateTaskStatus(@Param("ids") List<Long> taskIds, @Param("taskStatus") TaskStatus taskStatus);

    @Update("update task_info set retry_num = #{retryNum} where id = #{id}")
    void updateRetryNum(@Param("id") Long taskId, @Param("retryNum") Integer retryNum);

    @Select("select " + COLUMNS + " from task_info where task_status = #{taskStatus}")
    List<TaskEntity> findTaskByStatus(@Param("taskStatus") TaskStatus taskStatus);

    @Select("<script>"
            + " select " + COLUMNS + " from task_info where task_status in"
            + " <foreach item='item' index='index' collection='taskStatusList' open='(' separator=',' close=')'>"
            + "   #{item}"
            + " </foreach>"
            + "</script>")
    List<TaskEntity> findTaskByStatusIn(@Param("taskStatusList") List<TaskStatus> taskStatusList);

    @Update("update task_info set finished_time = #{finishedTime} where id = #{taskId}")
    void updateTaskFinishedTime(@Param("taskId") Long taskId, @Param("finishedTime") Date finishedTime);

    @Update("update task_info set started_time = #{startedTime} where id = #{taskId}")
    void updateTaskStartedTime(@Param("taskId") Long taskId, @Param("startedTime") Date startedTime);

    @Select("select " + COLUMNS + " from task_info where step_id = #{stepId}")
    List<TaskEntity> findByStepId(@Param("stepId") Long stepId);

    @Update("update task_info set task_request = #{request} where id = #{taskId}")
    void updateTaskRequest(@Param("taskId") Long taskId, @Param("request") String request);
}

