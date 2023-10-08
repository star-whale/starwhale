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

package ai.starwhale.mlops.domain.job.template.mapper;

import ai.starwhale.mlops.domain.job.template.po.TemplateEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TemplateMapper {
    String COLUMNS = "id, name, job_id, project_id, owner_id, created_time, modified_time,"
            + "IF(deleted_time > 0, 1, 0) as is_deleted";

    @Insert("INSERT into job_template(name, job_id, project_id, owner_id) "
            + "VALUES(#{name}, #{jobId}, #{projectId}, #{ownerId})")
    @Options(keyColumn = "id", useGeneratedKeys = true, keyProperty = "id")
    int insert(TemplateEntity template);

    @Update("update job_template set deleted_time = UNIX_TIMESTAMP(NOW(3)) * 1000 where id = #{id}")
    int remove(@Param("id") Long id);

    @Update("update job_template set deleted_time = 0 where id = #{id}")
    int recover(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + " FROM job_template WHERE id = #{id}")
    TemplateEntity selectById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + " FROM job_template "
            + "WHERE name = #{name} and project_id = #{projectId} and deleted_time = 0 "
            + "FOR UPDATE")
    TemplateEntity selectByNameForUpdate(@Param("name") String name, @Param("projectId") Long projectId);

    @Select("select " + COLUMNS + " from job_template where id = #{id} and deleted_time > 0")
    TemplateEntity selectDeletedById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + " FROM job_template "
            + "WHERE project_id = #{projectId} and deleted_time = 0 order by id desc")
    List<TemplateEntity> selectAll(@Param("projectId") Long projectId);

    @Select("SELECT " + COLUMNS + " FROM job_template "
            + "WHERE project_id = #{projectId} and deleted_time = 0 order by id desc limit #{limit}")
    List<TemplateEntity> select(@Param("projectId") Long projectId, @Param("limit") int limit);

    @Select("SELECT count(0) FROM job_template "
            + "WHERE project_id = #{projectId} and name = #{name} and deleted_time = 0")
    int selectExists(@Param("projectId") Long projectId, @Param("name") String name);
}
