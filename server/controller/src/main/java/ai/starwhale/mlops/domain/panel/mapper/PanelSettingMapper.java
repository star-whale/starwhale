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

package ai.starwhale.mlops.domain.panel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface PanelSettingMapper {
    @Select("select content from panel_setting where project_id=#{projectId} and name=#{name}")
    String get(long projectId, String name);

    @Insert("insert into panel_setting(user_id, project_id, name, content) "
            + "values (#{userId}, #{projectId}, #{name}, #{content}) on duplicate key "
            + "update user_id=#{userId},content=#{content}")
    void set(long userId, long projectId, String name, String content);
}
