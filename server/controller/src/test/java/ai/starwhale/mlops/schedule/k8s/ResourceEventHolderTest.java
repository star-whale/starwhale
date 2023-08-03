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

import ai.starwhale.mlops.schedule.impl.k8s.ResourceEventHolder;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceEventHolderTest {
    private ResourceEventHolder resourceEventHolder;
    private CoreV1Event event;

    @BeforeEach
    void setUp() {
        resourceEventHolder = new ResourceEventHolder(3600);
        event = new CoreV1Event()
                .metadata(new V1ObjectMeta().name("event-name"))
                .eventTime(OffsetDateTime.now())
                .count(1)
                .involvedObject(new V1ObjectReference().kind("Pod").name("test-pod"))
                .message("message")
                .reason("reason");
    }

    @Test
    void getEvents() {
        resourceEventHolder.onAdd(event);
        var events = resourceEventHolder.getEvents("Pod", "test-pod");
        Assertions.assertEquals(1, events.size());
        var expectedEvent = ResourceEventHolder.Event.builder()
                .name(event.getMetadata().getName())
                .eventTimeInMs(event.getEventTime().toInstant().toEpochMilli())
                .message(event.getMessage())
                .reason(event.getReason())
                .type(event.getType())
                .count(event.getCount())
                .object("Pod/test-pod")
                .build();
        Assertions.assertEquals(expectedEvent, events.get(0));

        events = resourceEventHolder.getEvents("Pod", "can not be found");
        Assertions.assertEquals(0, events.size());

        events = resourceEventHolder.getEvents("Kind can not be found", "test-pod");
        Assertions.assertEquals(0, events.size());

        events = resourceEventHolder.getPodEvents("test-pod");
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(expectedEvent, events.get(0));
    }

    @Test
    void onAdd() {
        resourceEventHolder.onAdd(event);
        var events = resourceEventHolder.getEvents("Pod", "test-pod");
        Assertions.assertEquals(1, events.size());

        var unknownKindOfEvent = event;
        unknownKindOfEvent.getInvolvedObject().kind("ReplicaSet");
        resourceEventHolder.onAdd(event);
        events = resourceEventHolder.getEvents("ReplicaSet", "test-pod");
        Assertions.assertEquals(0, events.size());
    }

    @Test
    void gc() throws InterruptedException {
        resourceEventHolder = new ResourceEventHolder(1);
        resourceEventHolder.onAdd(event);
        var events = resourceEventHolder.getEvents("Pod", "test-pod");
        Assertions.assertEquals(1, events.size());
        Thread.sleep(1000);
        resourceEventHolder.gc();
        events = resourceEventHolder.getEvents("Pod", "test-pod");
        Assertions.assertEquals(0, events.size());
    }
}
