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

package ai.starwhale.mlops.domain.settings.mapper;

import ai.starwhale.mlops.domain.settings.Scope;
import ai.starwhale.mlops.domain.settings.po.SettingsEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SettingsMapper {

    @Select("select * from settings where scope=#{scope} and owner_id=#{owner}")
    SettingsEntity get(Scope scope, Long owner);

    @Insert("insert into settings(content, owner_id, scope) values(#{content}, #{owner}, #{scope})")
    int insert(Long owner, String content, Scope scope);

    @Update("update settings set content=#{content} where id=#{id}")
    int update(Long id, String content);

}
