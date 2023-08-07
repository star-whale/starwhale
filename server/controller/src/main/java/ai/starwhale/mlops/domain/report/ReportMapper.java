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

package ai.starwhale.mlops.domain.report;

import java.util.List;
import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.jdbc.SQL;


@Mapper
public interface ReportMapper {
    String COLUMNS_FOR_INSERT = "uuid, name, content, project_id, creator_id, shared";
    String COLUMNS_FOR_SELECT = "id, created_time, modified_time, " + COLUMNS_FOR_INSERT + ", "
            + "IF(deleted_time > 0, 1, 0) as is_deleted, "
            + "IF(deleted_time > 0, deleted_time, null) as deleted_time";;

    @Insert("INSERT INTO report (" + COLUMNS_FOR_INSERT + ") "
            + "VALUES (#{uuid}, #{name}, #{content}, #{projectId}, #{creatorId}, #{shared})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReportEntity entity);

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM report WHERE project_id = #{projectId}")
    List<ReportEntity> selectByProject(@Param("projectId") Long projectId);

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM report WHERE id = #{id}")
    ReportEntity selectById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM report WHERE uuid = #{uuid}")
    ReportEntity selectByUuid(@Param("uuid") String uuid);

    @SelectProvider(value = SqlProvider.class, method = "findByNameSql")
    ReportEntity selectByName(@Param("name") String name, @Param("projectId") Long projectId,
                              @Param("forUpdate") Boolean forUpdate);

    @Update("UPDATE report SET shared = #{shared} where id = #{id}")
    int updateShared(@Param("id") Long id, @Param("shared") Boolean shared);

    @Update("UPDATE report SET shared = #{shared} where uuid = #{uuid}")
    int updateSharedByUuid(@Param("uuid") String uuid, @Param("shared") Boolean shared);

    @Update("UPDATE report SET content = #{content} where id = #{id}")
    int updateContent(@Param("id") Long id, @Param("content") String content);

    @Update("UPDATE report SET content = #{content} where uuid = #{uuid}")
    int updateContentByUuid(@Param("uuid") String uuid, @Param("content") String content);

    @UpdateProvider(value = SqlProvider.class, method = "updateSql")
    int update(@Param("id") Long id,
               @Param("uuid") String uuid,
               @Param("content") String content,
               @Param("shared") Boolean shared);

    @Update("UPDATE report SET project_id = #{projectId} where id = #{id}")
    int updateToNewProject(@Param("id") Long id, @Param("projectId") Long projectId);

    @Update("update report set deleted_time = UNIX_TIMESTAMP(NOW(3)) * 1000 where id = #{id}")
    int remove(@Param("id") Long id);

    @Update("update report set deleted_time = 0 where id = #{id}")
    int recover(@Param("id") Long id);
    @Select("select " + COLUMNS_FOR_SELECT + " from report where deleted_time > 0 and id = #{id}")
    ReportEntity findDeleted(@Param("id") Long id);

    class SqlProvider {
        public String updateSql(@Param("id") Long id,
                                @Param("uuid") String uuid,
                                @Param("shared") Boolean shared,
                                @Param("content") String content) {
            return new SQL() {
                {
                    UPDATE("report");
                    if (StrUtil.isNotEmpty(content)) {
                        SET("content = #{content}");
                    }
                    if (shared != null) {
                        SET("shared = #{shared}");
                    }
                    if (id != null) {
                        WHERE("id = #{id}");
                    }
                    if (StrUtil.isNotEmpty(uuid)) {
                        WHERE("uuid = #{uuid}");
                    }
                }
            }.toString();
        }

        public String findByNameSql(@Param("name") String name,
                                    @Param("projectId") Long projectId,
                                    @Param("forUpdate") Boolean forUpdate) {
            String sql = new SQL() {
                {
                    SELECT(COLUMNS_FOR_SELECT);
                    FROM("report");
                    WHERE("name = #{name}");
                    WHERE("deleted_time = 0");
                    WHERE("project_id = #{projectId}");
                }
            }.toString();
            return Objects.equals(forUpdate, true) ? (sql + " for update") : sql;
        }
    }
}
