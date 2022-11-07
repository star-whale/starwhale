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

package ai.starwhale.mlops.domain.trash.mapper;

import ai.starwhale.mlops.domain.trash.po.TrashPo;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrashMapper {

    @Select("insert into trash(project_id, object_id, operator_id,"
            + " trash_name, trash_type, size, retention,updated_time)"
            + " values (#{projectId}, #{objectId}, #{operatorId},"
            + " #{trashName}, #{trashType}, #{size}, #{retention}, #{updatedTime})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(TrashPo po);

    @Delete("delete from trash where id = #{id}")
    int delete(Long id);

    @Select("select id, project_id, object_id, operator_id, trash_name, trash_type, size, retention,"
            + " updated_time, created_time, modified_time"
            + " from trash where id = #{id}")
    TrashPo find(Long id);

    @Select("<script>"
            + " select id, project_id, object_id, operator_id, trash_name, trash_type, size, retention,"
            + " updated_time, created_time, modified_time"
            + " from trash"
            + " where project_id = #{projectId}"
            + "    <if test=\"operatorId != null\">"
            + "      and operator_id = #{operatorId}"
            + "    </if>"
            + "    <if test=\"name != null and name != ''\">"
            + "      and trash_name like concat(#{name}, '%')"
            + "    </if>"
            + "    <if test=\"type != null and type != ''\">"
            + "      and trash.trash_type = #{type}"
            + "    </if>"
            + " order by id desc"
            + "</script>")
    List<TrashPo> list(@Param("projectId") Long projectId, @Param("operatorId") Long operatorId,
            @Param("name") String name, @Param("type") String type);
}
