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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.impl.k8s.reporting.JobEventHandler;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobEventHandlerTest {

    RunReportReceiver runReportReceiver;
    JobEventHandler jobEventHandler;
    private final K8sClient k8sClient = mock(K8sClient.class);
    private final OffsetDateTime startTime = OffsetDateTime.now().minusMinutes(1);
    private final OffsetDateTime endTime = startTime.plusSeconds(10);

    @BeforeEach
    public void setUp() throws ApiException {
        runReportReceiver = mock(RunReportReceiver.class);
        jobEventHandler = new JobEventHandler(runReportReceiver, k8sClient);

        var pod = new V1Pod().metadata(new V1ObjectMeta().name("1"));
        pod.setStatus(new V1PodStatus().startTime(startTime));
        when(k8sClient.getPodsByJobName("1")).thenReturn(new V1PodList().addItemsItem(pod));
        when(k8sClient.getPodsByJobNameQuietly("1")).thenReturn(List.of(pod));
    }

    @Test
    public void testOnAddSuccess() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1"));
        var completeTime = OffsetDateTime.now();
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(
                List.of(new V1JobCondition().status("True").type("Complete").lastTransitionTime(completeTime)));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onAdd(v1Job);
        var expected = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.FINISHED)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(completeTime.toInstant().toEpochMilli())
                .build();
        verify(runReportReceiver).receive(expected);
    }

    @Test
    public void testOnAddFail() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1"));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setActive(null);
        v1JobStatus.setFailed(1);
        v1JobStatus.setConditions(
                List.of(new V1JobCondition().status("True").type("Failed").lastTransitionTime(endTime)));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onAdd(v1Job);
        var expected = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.FAILED)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .build();
        verify(runReportReceiver).receive(expected);

        // test with reason and message
        var con = new V1JobCondition()
                .status("True").type("Failed").lastTransitionTime(endTime).reason("reason").message("message");
        v1JobStatus.setConditions(List.of(con));
        jobEventHandler.onAdd(v1Job);
        var expected2 = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.FAILED)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .failedReason("job failed: reason, message")
                .build();
        verify(runReportReceiver).receive(expected2);

        // prefer using pod status
        var pod = new V1Pod().metadata(new V1ObjectMeta().name("1"));
        pod.setStatus(new V1PodStatus().startTime(startTime).reason("foo").message("bar").phase("Failed"));
        reset(k8sClient);
        when(k8sClient.getPodsByJobNameQuietly("1")).thenReturn(List.of(pod, pod));
        jobEventHandler.onAdd(v1Job);
        var expected3 = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.FAILED)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .failedReason("job failed: reason, message\npod failed: foo, bar\nfoo, bar")
                .build();
        verify(runReportReceiver).receive(expected3);
    }

    @Test
    public void testOnUpdateSuccess() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1"));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setSucceeded(1);
        v1JobStatus.setConditions(List.of(new V1JobCondition().status("True").type("Complete").lastTransitionTime(
                endTime)));
        v1Job.setStatus(v1JobStatus);
        jobEventHandler.onUpdate(null, v1Job);
        var expected = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.FINISHED)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .ip(null)
                .build();
        verify(runReportReceiver).receive(expected);
    }

    @Test
    public void testOnUpdateFail() {
        V1Job v1Job = new V1Job();
        v1Job.setMetadata(new V1ObjectMeta().name("1"));
        V1JobStatus v1JobStatus = new V1JobStatus();
        v1JobStatus.setActive(null);
        v1JobStatus.setFailed(1);
        v1JobStatus.setConditions(
                List.of(new V1JobCondition().status("True").type("Failed").lastTransitionTime(endTime)));
        v1Job.setStatus(v1JobStatus);

        jobEventHandler.onUpdate(null, v1Job);
        var expected = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.FAILED)
                .startTimeMillis(startTime.toInstant().toEpochMilli())
                .stopTimeMillis(endTime.toInstant().toEpochMilli())
                .build();
        verify(runReportReceiver).receive(expected);
        verify(k8sClient, times(2)).getPodsByJobNameQuietly("1");
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
        verify(runReportReceiver, times(0)).receive(any());
    }

}
