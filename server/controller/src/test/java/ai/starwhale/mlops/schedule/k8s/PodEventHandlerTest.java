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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.build.log.BuildLogCollector;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.impl.k8s.log.TaskLogK8sCollector;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import ai.starwhale.mlops.schedule.impl.k8s.reporting.PodEventHandler;
import io.kubernetes.client.openapi.models.V1ContainerState;
import io.kubernetes.client.openapi.models.V1ContainerStateTerminated;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PodEventHandlerTest {

    PodEventHandler podEventHandler;

    TaskLogK8sCollector taskLogK8sCollector;
    BuildLogCollector buildLogCollector;
    TaskReportReceiver taskReportReceiver;

    HotJobHolder hotJobHolder;

    V1Pod v1Pod;

    @BeforeEach
    public void setup() {
        hotJobHolder = mock(HotJobHolder.class);
        taskLogK8sCollector = mock(TaskLogK8sCollector.class);
        buildLogCollector = mock(BuildLogCollector.class);
        taskReportReceiver = mock(TaskReportReceiver.class);
        podEventHandler = new PodEventHandler(
                taskLogK8sCollector, buildLogCollector, taskReportReceiver, hotJobHolder, mock(DatasetService.class));
        v1Pod = new V1Pod()
                .metadata(new V1ObjectMeta()
                        .labels(Map.of("job-name", "3", "job-type", "eval")).name("3-xxx"))
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
        v1Pod.getStatus().podIP("127.0.0.1");
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector, times(0)).collect(any());
        verify(taskReportReceiver, times(1)).receive(any());
        verify(taskReportReceiver).receive(argThat(tasks ->
                tasks.size() == 1
                        && tasks.get(0).getId() == 3L
                        && tasks.get(0).getStatus() == TaskStatus.PREPARING
                        && Objects.equals(tasks.get(0).getIp(), "127.0.0.1")));
    }

    @Test
    public void testTaskNotFound() {
        when(hotJobHolder.tasksOfIds(List.of(3L))).thenReturn(List.of());
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector, times(0)).collect(any());
    }

    @Test
    public void testPodScheduled() {
        v1Pod.getStatus().getContainerStatuses().get(0).getState().terminated(null);
        v1Pod.getStatus().phase("Pending");
        v1Pod.getStatus().conditions(List.of(new V1PodCondition().status("True").type("PodScheduled")));
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector, times(0)).collect(any());
        var expect = ReportedTask.builder()
                .id(3L)
                .status(TaskStatus.PREPARING)
                .build();
        verify(taskReportReceiver, times(1)).receive(List.of(expect));
    }

    @Test
    public void testTerminating() {
        v1Pod.getMetadata().setDeletionTimestamp(OffsetDateTime.now());
        v1Pod.getStatus().setPhase("Running");
        podEventHandler.onUpdate(null, v1Pod);
        verify(taskLogK8sCollector, never()).collect(any());
        verify(taskReportReceiver, never()).receive(any());
    }
}
