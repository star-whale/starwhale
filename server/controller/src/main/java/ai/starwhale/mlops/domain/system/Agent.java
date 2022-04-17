/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.domain.node.Node;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * bo represent agent
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Agent {

    Long id;
    String ip;

    public static Agent fromEntity(AgentEntity entity){
        if(null == entity){
            return null;
        }
        return Agent.builder().id(entity.getId()).ip(entity.getAgentIp()).build();
    }

    public static Agent fromNode(Node node){
        return Agent.builder().ip(node.getIpAddr()).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Agent agent = (Agent) o;
        return Objects.equals(id, agent.id) ||
            ip.equals(agent.ip);
    }

    public Agent copy(){
        return new Agent(id,ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }
}
