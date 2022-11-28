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

package ai.starwhale.mlops.domain.system.agent;

import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.agent.bo.Node;
import ai.starwhale.mlops.domain.system.agent.bo.NodeInfo;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * convert Agent between Node and AgentEntity
 */
@Slf4j
@Component
public class AgentConverter {

    final ObjectMapper objectMapper;

    public AgentConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Agent fromNode(Node node) {
        return Agent.builder()
                .ip(node.getIpAddr())
                .serialNumber(node.getSerialNumber())
                .nodeInfo(new NodeInfo(node.getMemorySizeGb(), node.getDevices()))
                .agentVersion(node.getAgentVersion())
                .status(node.getStatus())
                .connectTime(Instant.now().toEpochMilli())
                .build();
    }

    public Agent fromEntity(AgentEntity entity) {
        if (null == entity) {
            return null;
        }
        NodeInfo nodeInfo = null;
        try {
            String deviceInfo = entity.getDeviceInfo();
            nodeInfo = objectMapper.readValue(deviceInfo == null ? "{}" : deviceInfo,
                    NodeInfo.class);
        } catch (JsonProcessingException e) {
            log.error("read devices from db failed {}", entity.getId(), e);
        }
        return Agent.builder()
                .id(entity.getId())
                .ip(entity.getAgentIp())
                .serialNumber(entity.getSerialNumber())
                .agentVersion(entity.getAgentVersion())
                .status(entity.getAgentStatus())
                .nodeInfo(nodeInfo)
                .connectTime(entity.getConnectTime().getTime())
                .build();
    }

    public AgentEntity toEntity(Agent agent) {
        String deviceInfo = null;
        try {
            deviceInfo = objectMapper.writeValueAsString(agent.getNodeInfo());
        } catch (JsonProcessingException e) {
            log.error("write devices to db failed {}", agent.getSerialNumber(), e);
        }
        return AgentEntity.builder()
                .id(agent.getId())
                .serialNumber(agent.getSerialNumber())
                .agentIp(agent.getIp())
                .agentVersion(agent.getAgentVersion())
                .agentStatus(agent.getStatus())
                .connectTime(new Date(agent.getConnectTime()))
                .deviceInfo(deviceInfo)
                .build();
    }
}
