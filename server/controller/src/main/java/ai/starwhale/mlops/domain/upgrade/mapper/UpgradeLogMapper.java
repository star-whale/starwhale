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

package ai.starwhale.mlops.domain.upgrade.mapper;

import ai.starwhale.mlops.domain.upgrade.po.UpgradeLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UpgradeLogMapper {

    @Insert("insert into upgrade_log(progress_uuid, step_current, step_total, title, content, status) "
            + "values (#{progressUuid}, #{stepCurrent}, #{stepTotal}, #{title}, #{content}, #{status})")
    void insert(UpgradeLogEntity entity);

    @Update("update upgrade_log set status = #{status}"
            + " where progress_uuid = #{progressUuid} and step_current = #{stepCurrent}")
    void update(UpgradeLogEntity entity);

    @Select("select id, progress_uuid, step_current, step_total, title, content, status, created_time, modified_time"
            + " from upgrade_log"
            + " where progress_uuid = #{uuid}")
    List<UpgradeLogEntity> list(@Param("uuid") String uuid);
}
