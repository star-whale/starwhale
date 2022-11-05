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

import ai.starwhale.mlops.domain.dataset.dataloader.po.SessionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SessionMapper {

    @Insert("INSERT into dataset_read_session ("
            + "id, batch_size, dataset_name, dataset_version, table_name, "
            + "start, start_inclusive, end, end_inclusive) "
            + "VALUES("
            + "#{id}, #{batchSize}, #{datasetName}, #{datasetVersion}, #{tableName}, "
            + "#{start}, #{startInclusive}, #{end}, #{endInclusive}) "
            )
    int insert(SessionEntity session);

    @Select("SELECT * from dataset_read_session WHERE id=#{id}")
    SessionEntity selectById(String id);

    @Select("SELECT * from dataset_read_session WHERE id=#{id} FOR UPDATE")
    SessionEntity selectForUpdate(String id);

    @Select("SELECT * from dataset_read_session")
    List<SessionEntity> select();
}
