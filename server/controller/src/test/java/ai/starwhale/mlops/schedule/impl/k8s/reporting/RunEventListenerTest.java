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


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.event.EventService;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunEventListenerTest {
    private EventService eventService;
    private K8sClient k8sClient;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        k8sClient = mock(K8sClient.class);
    }

    @Test
    void testOnEvent() throws ApiException {
        var listener = new RunEventListener(this.eventService, this.k8sClient);
        var eventBuilder = ResourceEventHolder.Event.builder()
                .name("test-pod")
                .eventTimeInMs(1L)
                .message("message")
                .reason("reason")
                .type("type")
                .count(1)
                .kind("Pod");

        listener.onEvent(eventBuilder.build());
        // the pod name is not a starwhale pod, so we should not add event
        verify(k8sClient, never()).getPod(any());
        verify(eventService, never()).addEvent(any());

        listener.onEvent(eventBuilder.name("starwhale-run-1-abc").build());
        verify(k8sClient).getPod("starwhale-run-1-abc");
        // the pod is empty, we can not get the run id from it
        verify(eventService, never()).addEvent(any());

        reset(k8sClient);
        listener.onEvent(eventBuilder.kind("Job").build());
        // we do not care about job events
        verify(k8sClient, never()).getPod(any());
        verify(eventService, never()).addEvent(any());

        // full version test
        reset(k8sClient);
        reset(eventService);
        var pod = new V1Pod()
                .metadata(new V1ObjectMeta().name("starwhale-run-1-abc")
                        .annotations(Map.of("starwhale.ai/run-id", "42")));
        when(k8sClient.getPod("starwhale-run-1-abc")).thenReturn(pod);
        listener.onEvent(eventBuilder.name("starwhale-run-1-abc").kind("Pod").build());
        verify(k8sClient).getPod("starwhale-run-1-abc");
        verify(eventService).addEvent(any());
    }
}
