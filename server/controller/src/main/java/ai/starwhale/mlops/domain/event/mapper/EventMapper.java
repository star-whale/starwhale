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

package ai.starwhale.mlops.domain.event.mapper;

import ai.starwhale.mlops.api.protocol.event.Event;
import ai.starwhale.mlops.domain.event.po.EventEntity;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventMapper {
    String COLUMNS = "id, type, source, resource, resource_id, message, data, created_time";
    String TABLE = "event";

    @Insert("insert into " + TABLE
            + " (type, source, resource, resource_id, message, data, created_time)"
            + " values"
            + " (#{type}, #{source}, #{resource}, #{resourceId}, #{message}, #{data}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(@NotNull EventEntity event);

    @Select("select " + COLUMNS + " from " + TABLE
            + " where resource = #{resource} and resource_id = #{resourceId} order by created_time asc")
    List<EventEntity> listEvents(@NotNull Event.EventResource resource, @NotNull Long resourceId);
}
