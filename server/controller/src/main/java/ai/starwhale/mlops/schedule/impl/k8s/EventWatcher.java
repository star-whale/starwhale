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

package ai.starwhale.mlops.schedule.impl.k8s;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventWatcher {
    final K8sClient k8sClient;
    final ResourceEventHandler<V1Job> eventHandlerJob;
    final ResourceEventHandler<V1Node> eventHandlerNode;
    final ResourceEventHandler<V1Pod> eventHandlerPod;
    final ResourceEventHandler<CoreV1Event> resourceEventHolder;

    public EventWatcher(K8sClient k8sClient,
                        ResourceEventHandler<V1Job> eventHandlerJob,
                        ResourceEventHandler<V1Node> eventHandlerNode,
                        ResourceEventHandler<V1Pod> eventHandlerPod,
                        ResourceEventHolder resourceEventHolder) {
        this.k8sClient = k8sClient;
        this.eventHandlerJob = eventHandlerJob;
        this.eventHandlerNode = eventHandlerNode;
        this.eventHandlerPod = eventHandlerPod;
        this.resourceEventHolder = resourceEventHolder;
    }

    @EventListener
    public void handleContextReadyEvent(ApplicationReadyEvent event) {
        log.info("spring context ready now, watching events from k8s");
        // for monitor eval task and image builder processing
        k8sClient.watchJob(eventHandlerJob, K8sClient.toV1LabelSelector(K8sJobTemplate.starwhaleJobLabel));
        // for monitor log and some tasks' status
        k8sClient.watchPod(eventHandlerPod, K8sClient.toV1LabelSelector(K8sJobTemplate.starwhaleJobLabel));
        // for monitor resources of nodes
        k8sClient.watchNode(eventHandlerNode);
        // for common event needs
        k8sClient.watchEvent(resourceEventHolder, null);
    }
}
