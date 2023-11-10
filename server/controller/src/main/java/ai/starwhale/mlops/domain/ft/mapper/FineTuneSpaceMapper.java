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

import ai.starwhale.mlops.domain.ft.po.FineTuneSpaceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface FineTuneSpaceMapper {

    String COLUMNS = "id,           \n"
            + "project_id,      \n"
            + "owner_id,       \n"
            + "name,     \n"
            + "description,     \n"
            + "created_time, \n"
            + "modified_time ";

    @Insert("insert into fine_tune_space"
            + " (project_id, owner_id, name, description)"
            + " values (#{projectId}, #{ownerId}, #{name},#{description})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int add(FineTuneSpaceEntity spaceEntity);

    @Select("select " + COLUMNS + " from fine_tune_space where project_id = #{projectId} order by id desc")
    List<FineTuneSpaceEntity> list(Long projectId);


    @UpdateProvider(value = UpdateSqlProvider.class, method = "update")
    int update(Long spaceId, String name, String description);

    class UpdateSqlProvider {
        public static String update(Long spaceId, String name, String description) {
            return new SQL() {
                {
                    UPDATE("fine_tune_space");
                    if (null != name) {
                        SET("name=#{name}");
                    }
                    if (null != description) {
                        SET("description=#{description}");
                    }
                    WHERE("id=#{spaceId}");
                }
            }.toString();
        }
    }
}
