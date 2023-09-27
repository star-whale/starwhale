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

package ai.starwhale.mlops.domain.run.mapper;

import ai.starwhale.mlops.domain.run.RunEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface RunMapper {

    String COLUMNS = "id,           \n"
            + "task_id,      \n"
            + "status,       \n"
            + "log_dir,     \n"
            + "run_spec,     \n"
            + "ip,           \n"
            + "failed_reason,\n"
            + "start_time,   \n"
            + "finish_time,  \n"
            + "created_time, \n"
            + "modified_time ";

    @Results({
            @Result(property = "runSpec", column = "run_spec", typeHandler = RunSpecConvertor.class)
    })
    @Select("select " + COLUMNS + " from run where task_id = #{taskId} order by id")
    List<RunEntity> list(Long taskId);

    @Results({
            @Result(property = "runSpec", column = "run_spec", typeHandler = RunSpecConvertor.class)
    })
    @Select("select " + COLUMNS + " from run where id = #{id}")
    RunEntity get(Long id);

    @Results({
            @Result(property = "runSpec", column = "run_spec", typeHandler = RunSpecConvertor.class)
    })
    @Select("select " + COLUMNS + " from run where id = #{id} for update")
    RunEntity getForUpdate(Long id);

    @Insert("insert into run"
            + " (task_id, status, log_dir, run_spec)"
            + " values (#{taskId}, #{status}, #{logDir},"
            + " #{runSpec, typeHandler=ai.starwhale.mlops.domain.run.mapper.RunSpecConvertor})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void insert(RunEntity runEntity);

    class UpdateRunEntitySqlProvider {
        public static String update(RunEntity runEntity) {
            return new SQL() {
                {
                    UPDATE("run");
                    if (null != runEntity.getStatus()) {
                        SET("status=#{status}");
                    }
                    if (null != runEntity.getFailedReason()) {
                        SET("failed_reason=#{failedReason}");
                    }
                    if (null != runEntity.getStartTime()) {
                        SET("start_time=#{startTime}");
                    }
                    if (null != runEntity.getFinishTime()) {
                        SET("finish_time=#{finishTime}");
                    }
                    if (null != runEntity.getIp()) {
                        SET("ip=#{ip}");
                    }
                    WHERE("id=#{id}");
                }
            }.toString();
        }
    }

    @UpdateProvider(value = UpdateRunEntitySqlProvider.class, method = "update")
    void update(RunEntity runEntity);
}
