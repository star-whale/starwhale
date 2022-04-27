/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system.agent;

import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.AgentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
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
    public Agent fromNode(Node node){
        return Agent.builder()
            .ip(node.getIpAddr())
            .nodeInfo(new NodeInfo(node.getMemorySizeGB(),node.getDevices()))
            .agentVersion(node.getAgentVersion())
            .connectTime(System.currentTimeMillis())
            .build();
    }

    public Agent fromEntity(AgentEntity entity){
        if(null == entity){
            return null;
        }
        NodeInfo nodeInfo = null;
        try {
            nodeInfo = objectMapper.readValue(entity.getDeviceInfo(),NodeInfo.class);
        } catch (JsonProcessingException e) {
            log.error("read devices from db failed {}",entity.getId(),e);
        }
        return Agent.builder()
            .id(entity.getId())
            .ip(entity.getAgentIp())
            .agentVersion(entity.getAgentVersion())
            .nodeInfo(nodeInfo)
            .connectTime(entity.getConnectTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .build();
    }

    public AgentEntity toEntity(Agent agent){
        String deviceInfo = null;
        try {
            deviceInfo = objectMapper.writeValueAsString(agent.getNodeInfo());
        } catch (JsonProcessingException e) {
            log.error("write devices to db failed {}",agent.getIp(),e);
        }
        return AgentEntity.builder()
            .id(agent.getId())
            .agentIp(agent.getIp())
            .agentVersion(agent.getAgentVersion())
            .connectTime(Instant.ofEpochMilli(agent.getConnectTime()).atZone(ZoneId.systemDefault()).toLocalDateTime())
            .deviceInfo(deviceInfo)
            .build();
    }
}
