/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system.agent;

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
    /**
     * the version of the agent that is deployed on this node
     */
    String agentVersion;

    /**
     * the ip address of this node
     */
    String ip;

    /**
     * memory and device info
     */
    NodeInfo nodeInfo;

    Long connectTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Agent)) {
            return false;
        }
        Agent agent = (Agent) o;
        return Objects.equals(id, agent.id) ||
            ip.equals(agent.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }

    public static class AgentUnModifiable extends Agent{

        final Agent agent;
        public AgentUnModifiable(Agent agent) {
            this.agent = agent;
        }

        @Override
        public Long getId() {
            return agent.getId();
        }

        @Override
        public String getAgentVersion() {
            return agent.getAgentVersion();
        }

        @Override
        public String getIp() {
            return agent.getIp();
        }

        @Override
        public NodeInfo getNodeInfo() {
            return agent.getNodeInfo();
        }

        @Override
        public Long getConnectTime() {
            return agent.getConnectTime();
        }

        @Override
        public void setId(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAgentVersion(String agentVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setIp(String ip) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNodeInfo(NodeInfo nodeInfo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setConnectTime(Long connectTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return agent.toString();
        }

        @Override
        public boolean equals(Object o) {
            return agent.equals(o);
        }

        @Override
        public int hashCode() {
            return agent.hashCode();
        }
    }
}
