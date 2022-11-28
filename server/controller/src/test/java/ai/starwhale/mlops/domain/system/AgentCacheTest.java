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

package ai.starwhale.mlops.domain.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.system.agent.AgentCache;
import ai.starwhale.mlops.domain.system.agent.AgentConverter;
import ai.starwhale.mlops.domain.system.agent.AgentStatus;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.agent.bo.Node;
import ai.starwhale.mlops.domain.system.mapper.AgentMapper;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link ai.starwhale.mlops.domain.system.agent.AgentCache}
 */
public class AgentCacheTest {

    @Test
    public void testAgentCache() throws Exception {
        AgentMapper agentMapper = mock(AgentMapper.class);
        when(agentMapper.listAgents()).thenReturn(List.of(
                AgentEntity.builder()
                        .connectTime(new Date())
                        .id(1L)
                        .serialNumber("serialNumber1")
                        .agentIp("10.199.0.1")
                        .agentVersion("0.1")
                        .agentStatus(AgentStatus.ONLINE)
                        .build(),
                AgentEntity.builder()
                        .connectTime(new Date())
                        .id(2L)
                        .serialNumber("serialNumber2")
                        .agentIp("10.199.0.2")
                        .agentVersion("0.1")
                        .agentStatus(AgentStatus.ONLINE)
                        .build()
        ));
        AgentConverter agentConverter = new AgentConverter(new ObjectMapper());
        AgentCache agentCache = new AgentCache(agentMapper, agentConverter);

        agentCache.run();

        agentCache.nodeReport(
                Node.builder().status(AgentStatus.ONLINE).devices(List.of()).serialNumber("serialNumber1").build());
        agentCache.nodeReport(
                Node.builder().status(AgentStatus.OFFLINE).devices(List.of()).serialNumber("serialNumber2").build());
        agentCache.flushDb();
        List<Agent> agents = agentCache.agents();
        Assertions.assertEquals(2, agents.size());
        Agent agent1 = findAgent(agentCache, "serialNumber1");
        Agent agent2 = findAgent(agentCache, "serialNumber2");
        Assertions.assertNotNull(agent1);
        Assertions.assertNotNull(agent2);
        Assertions.assertEquals(AgentStatus.ONLINE, agent1.getStatus());
        Assertions.assertEquals(AgentStatus.OFFLINE, agent2.getStatus());
        verify(agentMapper).update(anyList());

        agentCache.nodeReport(
                Node.builder().status(AgentStatus.OFFLINE).devices(List.of()).serialNumber("serialNumber1").build());
        agents = agentCache.agents();
        Assertions.assertEquals(2, agents.size());
        Set<AgentStatus> agentStatuses = agents.stream().map(Agent::getStatus)
                .collect(Collectors.toSet());
        Assertions.assertEquals(1, agentStatuses.size());
        Assertions.assertTrue(agentStatuses.contains(AgentStatus.OFFLINE));

        agentCache.nodeReport(
                Node.builder().status(AgentStatus.ONLINE).devices(List.of()).serialNumber("serialNumber3").build());

        agents = agentCache.agents();
        Assertions.assertEquals(3, agents.size());
        Agent agent3 = findAgent(agentCache, "serialNumber3");
        verify(agentMapper).insert(any(AgentEntity.class));
        Assertions.assertEquals("serialNumber3", agent3.getSerialNumber());
        Assertions.assertEquals(AgentStatus.ONLINE, agent3.getStatus());


    }

    @Test
    public void testRemoveOfflineAgent() throws Exception {

        AgentMapper agentMapper = mock(AgentMapper.class);
        when(agentMapper.listAgents()).thenReturn(List.of(
                AgentEntity.builder()
                        .connectTime(new Date())
                        .id(1L)
                        .serialNumber("serialNumber1")
                        .agentIp("10.199.0.1")
                        .agentVersion("0.1")
                        .agentStatus(AgentStatus.ONLINE)
                        .build(),
                AgentEntity.builder()
                        .connectTime(new Date())
                        .id(2L)
                        .serialNumber("serialNumber2")
                        .agentIp("10.199.0.2")
                        .agentVersion("0.1")
                        .agentStatus(AgentStatus.ONLINE)
                        .build()
        ));
        AgentConverter agentConverter = new AgentConverter(new ObjectMapper());
        AgentCache agentCache = new AgentCache(agentMapper, agentConverter);

        agentCache.run();

        agentCache.nodeReport(
                Node.builder().status(AgentStatus.OFFLINE).devices(List.of()).serialNumber("serialNumber1").build());
        agentCache.flushDb();
        agentCache.removeOfflineAgent("serialNumber1");
        List<Agent> agents = agentCache.agents();
        Assertions.assertEquals(1, agents.size());
        Agent agent2 = agents.get(0);
        Assertions.assertEquals("serialNumber2", agent2.getSerialNumber());
        verify(agentMapper).delete(1L);

        Assertions.assertThrowsExactly(SwValidationException.class,
                () -> agentCache.removeOfflineAgent("serialNumber2"));


    }

    private Agent findAgent(AgentCache agentCache, String serialNumber) {
        return agentCache.agents().stream().filter(agent -> agent.getSerialNumber().equals(serialNumber)).findAny()
                .get();
    }
}
