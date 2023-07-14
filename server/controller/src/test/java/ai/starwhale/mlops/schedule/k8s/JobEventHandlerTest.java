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

import static ai.starwhale.mlops.schedule.k8s.K8sJobTemplate.JOB_TYPE_LABEL;
import static ai.starwhale.mlops.schedule.k8s.K8sJobTemplate.WORKLOAD_TYPE_EVAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.reporting.TaskModifyReceiver;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class JobEventHandlerTest {

    TaskModifyReceiver taskModifyReceiver;
    DatasetService datasetService;
    JobEventHandler jobEventHandler;
    private final K8sClient k8sClient = mock(K8sClient.class);
    private final OffsetDateTime startTime = OffsetDateTime.now().minusMinutes(1);
    private final OffsetDateTime endTime = startTime.plusSeconds(10);

    @BeforeEach
    public void setUp() throws ApiException {
        taskModifyReceiver = mock(TaskModifyReceiver.class);
        datasetService = mock(DatasetService.class);
        TaskStatusMachine taskStatusMachine = new TaskStatusMachine();
        jobEventHandler = new JobEventHandler(
                taskModifyReceiver, taskStatusMachine, mock(RuntimeService.class), datasetService, k8sClient);

        var pod = new V1Pod().metadata(new V1ObjectMeta().name("1"));
        pod.setStatus(new V1PodStatus().startTime(startTime));
        when(k8sClient.getPodsByJobName("1")).thenReturn(new V1PodList().addItemsItem(pod));
        when(k8sClient.getPodsByJobNameQuietly("1")).thenReturn(List.of(pod));
    }

    @Test
    public void testOnAddSuccess() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        var completeTime = OffsetDateTime.now();
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(
                List.of(new V1JobCondition().status("True").type("Complete").lastTransitionTime(completeTime)));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onAdd(v1Job);
        var expected = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.SUCCESS)
                .retryCount(0)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(completeTime.toInstant().toEpochMilli())
                .build();
        verify(taskModifyReceiver).receive(List.of(expected));
    }

    @Test
    public void testOnAddFail() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setActive(null);
        v1JobStatus.setFailed(1);
        v1JobStatus.setConditions(
                List.of(new V1JobCondition().status("True").type("Failed").lastTransitionTime(endTime)));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onAdd(v1Job);
        var expected = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.FAIL)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .retryCount(1)
                .build();
        verify(taskModifyReceiver).receive(List.of(expected));

        // test with reason and message
        var con = new V1JobCondition()
                .status("True").type("Failed").lastTransitionTime(endTime).reason("reason").message("message");
        v1JobStatus.setConditions(List.of(con));
        jobEventHandler.onAdd(v1Job);
        var expected2 = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.FAIL)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .failedReason("job failed: reason, message")
                .retryCount(1)
                .build();
        verify(taskModifyReceiver).receive(List.of(expected2));

        // prefer using pod status
        var pod = new V1Pod().metadata(new V1ObjectMeta().name("1"));
        pod.setStatus(new V1PodStatus().startTime(startTime).reason("foo").message("bar").phase("Failed"));
        reset(k8sClient);
        when(k8sClient.getPodsByJobNameQuietly("1")).thenReturn(List.of(pod, pod));
        jobEventHandler.onAdd(v1Job);
        var expected3 = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.FAIL)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .failedReason("job failed: reason, message\npod failed: foo, bar\nfoo, bar")
                .retryCount(1)
                .build();
        verify(taskModifyReceiver).receive(List.of(expected3));
    }

    @Test
    public void testOnUpdateSuccess() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("True").type("Complete").lastTransitionTime(
                endTime)));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onUpdate(null, v1Job);
        var expected = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.SUCCESS)
                .retryCount(0)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .ip(null)
                .build();
        verify(taskModifyReceiver).receive(List.of(expected));
    }

    @Test
    public void testOnUpdateFail() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setActive(null);
        v1JobStatus.setFailed(1);
        v1JobStatus.setConditions(
                List.of(new V1JobCondition().status("True").type("Failed").lastTransitionTime(endTime)));
        v1Job.setStatus(v1JobStatus);

        jobEventHandler.onUpdate(null, v1Job);
        var expected = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.FAIL)
                .retryCount(1)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .build();
        verify(taskModifyReceiver).receive(List.of(expected));
        verify(k8sClient, times(2)).getPodsByJobNameQuietly("1");
    }

    @Test
    public void testOnUpdateUnknown() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("False").type("Complete")));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onUpdate(null, v1Job);
        var expected = ReportedTask.builder()
                .id(1L)
                .status(TaskStatus.UNKNOWN)
                .retryCount(0)
                .build();
        verify(taskModifyReceiver).receive(List.of(expected));
    }

    @Test
    public void testOnDelete() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        v1Job.setStatus(new V1JobStatus().active(1));
        jobEventHandler.onDelete(v1Job, false);
        var args = ArgumentCaptor.forClass(List.class);
        verify(taskModifyReceiver).receive(args.capture());
        assertEquals(1, args.getValue().size());
        ReportedTask reportedTask = (ReportedTask) args.getValue().get(0);
        assertEquals(1L, reportedTask.getId());
        assertEquals(TaskStatus.CANCELED, reportedTask.getStatus());
        assertEquals(reportedTask.getStartTimeMillis(), startTime.toInstant().toEpochMilli());
        assertTrue(reportedTask.getStopTimeMillis() > startTime.toInstant().toEpochMilli());
    }
}
