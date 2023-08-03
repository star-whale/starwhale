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

package ai.starwhale.mlops.schedule.k8s;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.system.agent.AgentCache;
import ai.starwhale.mlops.schedule.impl.k8s.reporting.NodeEventHandler;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeAddress;
import io.kubernetes.client.openapi.models.V1NodeSpec;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import io.kubernetes.client.openapi.models.V1NodeSystemInfo;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeEventHandlerTest {

    NodeEventHandler nodeEventHandler;
    AgentCache agentCache;

    @BeforeEach
    public void setUp() {
        agentCache = mock(AgentCache.class);
        nodeEventHandler = new NodeEventHandler(agentCache);
    }

    @Test
    public void testOnAdd() {
        nodeEventHandler.onAdd(new V1Node()
                .spec(new V1NodeSpec().unschedulable(false))
                .status(new V1NodeStatus()
                        .nodeInfo(new V1NodeSystemInfo().systemUUID("sysuuit").kubeletVersion("kubv"))
                        .capacity(Map.of("memory", new Quantity("2G")))
                        .addAddressesItem(new V1NodeAddress().address("addr")))
                .metadata(new V1ObjectMeta().labels(Map.of("label1", "x", "label2", "y"))));
        verify(agentCache).nodeReport(any());
    }

    @Test
    public void testOnUpdate() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            nodeEventHandler.onUpdate(new V1Node(), null);
        });
        nodeEventHandler.onUpdate(null, new V1Node().metadata(new V1ObjectMeta()));
        verify(agentCache).nodeReport(any());
    }

    @Test
    public void testOnDelete() {
        nodeEventHandler.onDelete(new V1Node(), true);
        verify(agentCache).removeOfflineAgent(any());
    }

}
