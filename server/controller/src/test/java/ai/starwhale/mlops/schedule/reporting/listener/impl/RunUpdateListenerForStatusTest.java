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

package ai.starwhale.mlops.schedule.reporting.listener.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RunUpdateListenerForStatusTest {

    HotJobHolder hotJobHolder;

    SwTaskScheduler swTaskScheduler;

    TaskStatusMachine taskStatusMachine;
    TaskMapper taskMapper;

    RunUpdateListenerForStatus runReportReceiver;

    @BeforeEach
    public void setup() {
        hotJobHolder = mock(HotJobHolder.class);
        swTaskScheduler = mock(SwTaskScheduler.class);
        taskStatusMachine = new TaskStatusMachine();
        taskMapper = mock(TaskMapper.class);
        runReportReceiver = new RunUpdateListenerForStatus(
                hotJobHolder,
                swTaskScheduler,
                taskStatusMachine,
                taskMapper,
                3
        );
    }

    @Test
    public void testOnUpdate() {
        when(hotJobHolder.taskWithId(1L)).thenReturn(null);
        var reportdRun = Run.builder()
                .id(1L)
                .taskId(1L)
                .status(RunStatus.RUNNING)
                .ip("127.0.0.1")
                .build();
        runReportReceiver.onRunUpdate(reportdRun);
        verify(taskMapper, times(0)).updateTaskStatus(any(), any());
        Task task = Task.builder()
                .retryNum(0)
                .step(Step.builder().spec(StepSpec.builder().backOffLimit(3).build()).build())
                .build();
        task = Mockito.spy(task);
        when(hotJobHolder.taskWithId(1L)).thenReturn(task);
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(0)).updateStatus(any());
        task.setCurrentRun(Run.builder().id(2L).build());
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(0)).updateStatus(any());
        task.setCurrentRun(Run.builder().id(1L).status(RunStatus.RUNNING).build());
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(0)).updateStatus(any());
        reportdRun.setStatus(RunStatus.FAILED);
        task.setCurrentRun(Run.builder().id(1L).status(RunStatus.RUNNING).build());
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(1)).updateStatus(TaskStatus.RETRYING);
        task.setCurrentRun(Run.builder().id(1L).status(RunStatus.RUNNING).build());
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(2)).updateStatus(TaskStatus.RETRYING);
        task.setCurrentRun(Run.builder().id(1L).status(RunStatus.RUNNING).build());
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(3)).updateStatus(TaskStatus.RETRYING);
        task.setCurrentRun(Run.builder().id(1L).status(RunStatus.RUNNING).build());
        runReportReceiver.onRunUpdate(reportdRun);
        verify(task, times(1)).updateStatus(TaskStatus.FAIL);

    }

}
