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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.reporting.TaskStatusReceiver;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobEventHandlerTest {

    TaskStatusReceiver taskStatusReceiver;
    JobEventHandler jobEventHandler;

    @BeforeEach
    public void setUp() {
        taskStatusReceiver = mock(TaskStatusReceiver.class);
        jobEventHandler = new JobEventHandler(taskStatusReceiver, mock(RuntimeService.class));
    }

    @Test
    public void testOnAddSuccess() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("True").type("Complete")));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onAdd(v1Job);
        verify(taskStatusReceiver).receive(List.of(new ReportedTask(1L, TaskStatus.SUCCESS, 0)));
    }

    @Test
    public void testOnAddFail() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setActive(null);
        v1JobStatus.setFailed(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("True").type("Failed")));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onAdd(v1Job);
        verify(taskStatusReceiver).receive(List.of(new ReportedTask(1L, TaskStatus.FAIL, 1)));
    }

    @Test
    public void testOnUpdateSuccess() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("True").type("Complete")));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onUpdate(null, v1Job);
        verify(taskStatusReceiver).receive(List.of(new ReportedTask(1L, TaskStatus.SUCCESS, 0)));
    }

    @Test
    public void testOnUpdateFail() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1").labels(Map.of(JOB_TYPE_LABEL, WORKLOAD_TYPE_EVAL)));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setActive(null);
        v1JobStatus.setFailed(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("True").type("Failed")));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onUpdate(null, v1Job);
        verify(taskStatusReceiver).receive(List.of(new ReportedTask(1L, TaskStatus.FAIL, 1)));
    }

    @Test
    public void testOnUpdateUnknown() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1"));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("False").type("Complete")));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onUpdate(null, v1Job);
        verify(taskStatusReceiver).receive(List.of(new ReportedTask(1L, TaskStatus.UNKNOWN, 0)));
    }

}
