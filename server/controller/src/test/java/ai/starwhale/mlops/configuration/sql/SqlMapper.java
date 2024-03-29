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

package ai.starwhale.mlops.configuration.sql;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface SqlMapper {
    @Insert("INSERT INTO sw_user (name) VALUES (#{name})")
    int insert(String name);

    @Select("SELECT name FROM sw_user")
    List<String> selectAll();

    @Select("SELECT name FROM sw_user where id = #{id} and name = #{name}")
    String selectById(int id, String name);

    @Select("SELECT name FROM sw_user \n"
            + " where id = #{id} and name = #{name}")
    String selectByIdAndName(int id, String name);
}
