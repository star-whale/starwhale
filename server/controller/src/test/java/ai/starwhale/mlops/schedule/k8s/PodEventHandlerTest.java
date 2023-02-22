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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import ai.starwhale.mlops.reporting.TaskStatusReceiver;
import io.kubernetes.client.openapi.models.V1ContainerState;
import io.kubernetes.client.openapi.models.V1ContainerStateTerminated;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PodEventHandlerTest {

    PodEventHandler podEventHandler;

    TaskLogK8sCollector taskLogK8sCollector;
    TaskStatusReceiver taskStatusReceiver;

    HotJobHolder hotJobHolder;

    V1Pod v1Pod;

    @BeforeEach
    public void setup() {
        hotJobHolder = mock(HotJobHolder.class);
        taskLogK8sCollector = mock(TaskLogK8sCollector.class);
        taskStatusReceiver = mock(TaskStatusReceiver.class);
        podEventHandler = new PodEventHandler(taskLogK8sCollector, taskStatusReceiver, hotJobHolder);
        v1Pod = new V1Pod()
                .metadata(new V1ObjectMeta().labels(Map.of("job-name", "3")))
                .status(new V1PodStatus()
                        .containerStatuses(List.of(
                                new V1ContainerStatus().state(
                                        new V1ContainerState().terminated(new V1ContainerStateTerminated()))
                        )));
    }

    @Test
    public void testTerminated() {
        Task task = mock(Task.class);
        when(hotJobHolder.tasksOfIds(List.of(3L))).thenReturn(List.of(task));
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector).collect(task);
    }

    @Test
    public void testRunning() {
        v1Pod.getStatus().getContainerStatuses().get(0).getState().terminated(null);
        v1Pod.getStatus().phase("Pending");
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector, times(0)).collect(any());
        verify(taskStatusReceiver, times(1)).receive(any());
    }

    @Test
    public void testTaskNotFound() {
        when(hotJobHolder.tasksOfIds(List.of(3L))).thenReturn(List.of());
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector, times(0)).collect(any());
    }


}
