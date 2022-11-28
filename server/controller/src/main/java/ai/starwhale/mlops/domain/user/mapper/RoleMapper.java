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

package ai.starwhale.mlops.domain.user.mapper;

import ai.starwhale.mlops.domain.user.po.RoleEntity;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {

    String COLUMNS = "id, role_name, role_code, role_description";

    @Select("select " + COLUMNS + " from user_role_info")
    List<RoleEntity> list();

    @Select("select " + COLUMNS + " from user_role_info"
            + " where id = #{id}")
    RoleEntity find(@NotNull @Param("id") Long id);

}
