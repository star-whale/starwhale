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

import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.agent.bo.Agent.AgentUnModifiable;
import ai.starwhale.mlops.domain.system.agent.bo.Node;
import ai.starwhale.mlops.domain.system.mapper.AgentMapper;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * release pain of agent reporting
 */
@Slf4j
@Service
public class AgentCache implements CommandLineRunner {

    final AgentMapper agentMapper;

    final Map<String, Agent> agents;

    final AgentConverter agentConverter;

    public AgentCache(AgentMapper agentMapper, AgentConverter agentConverter) {
        this.agentMapper = agentMapper;
        this.agentConverter = agentConverter;
        agents = new ConcurrentHashMap<>();
    }

    public List<Agent> agents() {
        return agents.values().parallelStream().map(AgentUnModifiable::new).collect(
                Collectors.toList());
    }

    public void removeOfflineAgent(String agentSerialNumber) {
        Agent tobeDeleteAgent = agents.get(agentSerialNumber);
        if (null == tobeDeleteAgent) {
            return;
        }
        if (tobeDeleteAgent.getStatus() != AgentStatus.OFFLINE) {
            throw new SwValidationException(ValidSubject.NODE, "you can't remove online agent manually!");
        }
        agentMapper.delete(tobeDeleteAgent.getId());
        agents.remove(agentSerialNumber);
    }

    public Agent nodeReport(Node node) {
        log.debug("node reported {}", node.getSerialNumber());
        if (StringUtils.hasText(node.getSerialNumber())) {
            Agent agentReported = agentConverter.fromNode(node);
            Agent residentAgent = agents.get(node.getSerialNumber());
            if (null == residentAgent) {
                agents.put(node.getSerialNumber(), agentReported);
                agentReported = save(agentReported);
                return new AgentUnModifiable(agentReported);
            } else {
                residentAgent.setAgentVersion(agentReported.getAgentVersion());
                residentAgent.setStatus(agentReported.getStatus());
                residentAgent.setNodeInfo(agentReported.getNodeInfo());
                residentAgent.setConnectTime(agentReported.getConnectTime());
                return new AgentUnModifiable(residentAgent);
            }
        } else {
            return null;
        }
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void flushDb() {
        List<AgentEntity> agentEntities = agents.values().stream()
                .map(agentConverter::toEntity)
                .collect(Collectors.toList());
        if (agentEntities.isEmpty()) {
            return;
        }
        BatchOperateHelper.doBatch(agentEntities,
                entities -> agentMapper.update(entities.parallelStream().collect(
                        Collectors.toList())), 100);
        ;
    }

    private Agent save(Agent agentReported) {
        AgentEntity entity = agentConverter.toEntity(agentReported);
        agentMapper.insert(entity);
        agentReported.setId(entity.getId());
        return agentReported;
    }


    @Override
    public void run(String... args) throws Exception {
        initCache();
    }

    private void initCache() {
        List<AgentEntity> agentEntities = agentMapper.listAgents();
        agentEntities.parallelStream().forEach(entity -> {
            Agent agent = agentConverter.fromEntity(entity);
            agents.put(entity.getSerialNumber(), agent);
        });
    }
}
