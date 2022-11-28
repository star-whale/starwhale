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

package ai.starwhale.mlops.domain.system.mapper;

import ai.starwhale.mlops.domain.system.po.AgentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentMapper {

    @Select("select id, inet_ntoa(agent_ip) as agent_ip, connect_time,"
            + " agent_version, agent_status, serial_number, device_info, created_time, modified_time"
            + " from agent_info"
            + " order by connect_time desc")
    List<AgentEntity> listAgents();

    @Insert("insert into agent_info(agent_ip, connect_time, agent_version, agent_status, serial_number, device_info)"
            + " values (inet_aton(#{agent.agentIp}), #{agent.connectTime}, #{agent.agentVersion},"
            + " #{agent.agentStatus}, #{agent.serialNumber}, #{agent.deviceInfo})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    Long insert(@Param("agent") AgentEntity agent);

    @Delete("delete from agent_info where id = #{agentId}")
    void delete(@Param("agentId") Long agentId);

    @Update("<script>"
            + "<foreach item=\"item\" index=\"index\" collection=\"agents\""
            + " open=\" \" separator=\";\" close=\" \">"
            + " update agent_info set connect_time = #{item.connectTime},"
            + " agent_version = #{item.agentVersion},"
            + " agent_status = #{item.agentStatus},"
            + " agent_ip = inet_aton(#{item.agentIp}),"
            + " device_info = #{item.deviceInfo} WHERE id = #{item.id}"
            + "</foreach>"
            + "</script>")
    void update(@Param("agents") List<AgentEntity> agents);

}
