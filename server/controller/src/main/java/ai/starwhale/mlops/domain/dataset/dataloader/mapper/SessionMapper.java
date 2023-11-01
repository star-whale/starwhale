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

package ai.starwhale.mlops.domain.dataset.dataloader.mapper;

import ai.starwhale.mlops.domain.dataset.dataloader.Status;
import ai.starwhale.mlops.domain.dataset.dataloader.po.SessionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionMapper {

    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    @Insert("INSERT into dataset_read_session ("
            + "session_id, batch_size, dataset_name, dataset_version, table_name, "
            + "start, start_type, start_inclusive, end, end_type, end_inclusive, status) "
            + "VALUES("
            + "#{sessionId}, #{batchSize}, #{datasetName}, #{datasetVersion}, #{tableName}, "
            + "#{start}, #{startType}, #{startInclusive}, #{end}, #{endType}, #{endInclusive}, #{status}) "
            )
    int insert(SessionEntity session);

    @Update("UPDATE dataset_read_session SET status=#{status} WHERE id=#{id}")
    int updateStatus(Long id, Status.SessionStatus status);

    @Select("SELECT * from dataset_read_session "
            + "WHERE session_id=#{sessionId} and dataset_version=#{datasetVersion}")
    SessionEntity selectOne(String sessionId, String datasetVersion);

    @Select("SELECT * from dataset_read_session WHERE id=#{id}")
    SessionEntity selectById(Long id);

    @Select("SELECT * from dataset_read_session WHERE id=#{id} FOR UPDATE")
    SessionEntity selectForUpdate(Long id);

    @Select("SELECT * from dataset_read_session WHERE session_id=#{sessionId}")
    List<SessionEntity> selectBySessionId(String sessionId);

    @Select("SELECT * from dataset_read_session")
    List<SessionEntity> selectAll();

    @Select("SELECT * from dataset_read_session where status=#{status}")
    List<SessionEntity> selectByStatus(Status.SessionStatus status);
}
