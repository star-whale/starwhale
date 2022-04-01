/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentMapper {

    List<AgentEntity> listAgents();

    Long addAgent(AgentEntity agent);

    AgentEntity findByIp(@Param("ip")String ip);

    AgentEntity findByIpForUpdate(@Param("ip")String ip);

}
