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

import ai.starwhale.mlops.domain.user.po.UserEntity;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface UserMapper {

    String COLUMNS = "id, user_name, user_pwd, user_pwd_salt, user_enabled, created_time, modified_time";

    @Insert("insert into user_info (user_name, user_pwd, user_pwd_salt, user_enabled)"
            + " values (#{userName}, #{userPwd}, #{userPwdSalt}, #{userEnabled})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(@NotNull UserEntity user);

    @Select("select " + COLUMNS + " from user_info"
            + " where id = #{id}")
    UserEntity find(@NotNull @Param("id") Long id);

    @Select("select " + COLUMNS + " from user_info"
            + " where user_name = #{userName}")
    UserEntity findByName(@NotNull @Param("userName") String userName);

    @SelectProvider(value = UserProvider.class, method = "list")
    List<UserEntity> list(@Param("userName") String userName, @Param("order") String order);

    @Update("update user_info"
            + " set user_pwd = #{pwd}, user_pwd_salt = #{salt}"
            + " where id = #{id}")
    int updatePassword(@NotNull @Param("id") Long id,
            @NotNull @Param("pwd") String password,
            @NotNull @Param("salt") String salt);

    @Update("update user_info"
            + " set user_enabled = #{enabled}"
            + " where id = #{id}")
    int updateEnabled(@NotNull @Param("id") Long id, @Param("enabled") Integer enabled);

    class UserProvider {

        public String list(String userName, String order) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("user_info");
                    if (StrUtil.isNotEmpty(userName)) {
                        WHERE("user_name like concat(#{userNamePrefix}, '%')");
                    }
                    if (StrUtil.isNotEmpty(order)) {
                        ORDER_BY(order);
                    } else {
                        ORDER_BY("user_name asc");
                    }
                }
            }.toString();
        }
    }
}
