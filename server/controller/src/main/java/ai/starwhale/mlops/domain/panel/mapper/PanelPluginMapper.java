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

import ai.starwhale.mlops.domain.panel.po.PanelPluginEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface PanelPluginMapper {

    @Select("select id, name, version, meta from panel_plugin where deleted_time is null")
    List<PanelPluginEntity> list();

    @Insert(value = "insert into panel_plugin(name, version, meta) values (#{name}, #{version}, #{meta})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(PanelPluginEntity plugin);

    @Select("select id, name, version, meta from panel_plugin where name=#{name} and deleted_time is null")
    List<PanelPluginEntity> get(String name);

    @Select("select id, name, version, meta from panel_plugin where "
            + "name=#{name} and version=#{version} and deleted_time is null")
    PanelPluginEntity getByNameAndVersion(String name, String version);

    @Update("update panel_plugin set deleted_time=NOW() where id=#{id}")
    void remove(Long id);
}
