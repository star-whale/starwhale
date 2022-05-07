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

import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.AgentEntity;
import ai.starwhale.mlops.domain.system.agent.Agent.AgentUnModifiable;
import ai.starwhale.mlops.domain.system.mapper.AgentMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * release pain of agent reporting
 */
@Slf4j
@Service
public class AgentCache implements CommandLineRunner {

    final AgentMapper agentMapper;

    final Map<String,Agent> agents;

    final AgentConverter agentConverter;

    public AgentCache(AgentMapper agentMapper,
        AgentConverter agentConverter) {
        this.agentMapper = agentMapper;
        this.agentConverter = agentConverter;
        agents = new ConcurrentHashMap<>();
    }

    public List<Agent> agents(){
        return agents.values().parallelStream().map(agent -> new AgentUnModifiable(agent)).collect(
            Collectors.toList());
    }

    public Agent nodeReport(Node node){
        log.debug("node reported {}",node.getSerialNumber());
        Agent agentReported = agentConverter.fromNode(node);
        Agent residentAgent = agents.get(node.getSerialNumber());
        if(null == residentAgent){
            agents.put(node.getSerialNumber(),agentReported);
            agentReported = save(agentReported);
            return new AgentUnModifiable(agentReported);
        }else {
            residentAgent.setAgentVersion(agentReported.getAgentVersion());
            residentAgent.setNodeInfo(agentReported.getNodeInfo());
            residentAgent.setConnectTime(agentReported.getConnectTime());
            return new AgentUnModifiable(residentAgent);
        }
    }

    @Scheduled(initialDelay = 10000,fixedDelay = 30000)
    public void flushDb(){
        List<AgentEntity> agentEntities = agents.values().stream()
            .map(agent -> agentConverter.toEntity(agent))
            .collect(Collectors.toList());
        if(null == agentEntities || agentEntities.isEmpty()){
            return;
        }
        BatchOperateHelper.doBatch(agentEntities,entities->agentMapper.updateAgents(entities.parallelStream().collect(
            Collectors.toList())),100);;
    }

    private Agent save(Agent agentReported) {
        AgentEntity entity = agentConverter.toEntity(agentReported);
        agentMapper.addAgent(entity);
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
            agents.put(entity.getAgentIp(),agentConverter.fromEntity(entity));
        });
    }
}
