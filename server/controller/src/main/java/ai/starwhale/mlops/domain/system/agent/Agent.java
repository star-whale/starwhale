/**
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
     * the unique number to identify this node
     */
    String serialNumber;

    /**
     * memory and device info
     */
    NodeInfo nodeInfo;

    Long connectTime;

    AgentStatus status;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Agent)) {
            return false;
        }
        Agent agent = (Agent) o;
        return Objects.equals(id, agent.getId()) ||
            serialNumber.equals(agent.getSerialNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNumber);
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
        public String getSerialNumber() {
            return agent.getSerialNumber();
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
        public AgentStatus getStatus(){
            return agent.getStatus();
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
        public void setSerialNumber(String  serialNumber) {
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
