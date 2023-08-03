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

package ai.starwhale.mlops.schedule.impl.k8s.reporting;

import ai.starwhale.mlops.domain.system.agent.AgentCache;
import ai.starwhale.mlops.domain.system.agent.AgentStatus;
import ai.starwhale.mlops.domain.system.agent.bo.Node;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeSpec;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class NodeEventHandler implements ResourceEventHandler<V1Node> {

    final AgentCache agentCache;

    public NodeEventHandler(AgentCache agentCache) {
        this.agentCache = agentCache;
    }

    @Override
    public void onAdd(V1Node obj) {
        agentCache.nodeReport(k8sNodeToSwNode(obj));
    }

    @Override
    public void onUpdate(V1Node oldObj, V1Node newObj) {
        agentCache.nodeReport(k8sNodeToSwNode(newObj));
    }

    @Override
    public void onDelete(V1Node obj, boolean deletedFinalStateUnknown) {
        Node node = k8sNodeToSwNode(obj);
        agentCache.removeOfflineAgent(node.getSerialNumber());
    }

    Node k8sNodeToSwNode(V1Node k8sNode) {
        Node n = new Node();
        Boolean unschedulable = false;
        V1NodeSpec spec = k8sNode.getSpec();
        if (null != spec && null != spec.getUnschedulable()) {
            unschedulable = spec.getUnschedulable();
        }
        n.setStatus(unschedulable ? AgentStatus.OFFLINE : AgentStatus.ONLINE);
        V1NodeStatus status = k8sNode.getStatus();
        if (status != null) {
            if (null != status.getCapacity() && null != status.getCapacity().get("memory")
                    && null != status.getCapacity().get("memory").getNumber()) {
                n.setMemorySizeGb(
                        status.getCapacity().get("memory").getNumber().divide(BigDecimal.valueOf(1024 * 1024L))
                                .floatValue());
            }
            if (null != status.getAddresses() && status.getAddresses().size() > 0) {
                n.setIpAddr(status.getAddresses().get(0).getAddress());
            }

            if (null != status.getNodeInfo()) {
                n.setSerialNumber(status.getNodeInfo().getSystemUUID());
                n.setAgentVersion("KUBELET:" + status.getNodeInfo().getKubeletVersion());
            }
        }
        return n;
    }
}
