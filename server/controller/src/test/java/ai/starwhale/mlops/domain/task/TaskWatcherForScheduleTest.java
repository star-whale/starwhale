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

package ai.starwhale.mlops.domain.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForSchedule;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.log.TaskLogSaver;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * test for {@link TaskWatcherForSchedule}
 */
public class TaskWatcherForScheduleTest {

    final TaskStatusMachine taskStatusMachine = new TaskStatusMachine();

    TaskLogSaver taskLogSaver = mock(TaskLogSaver.class);

    TaskReportReceiver taskReportReceiver = mock(TaskReportReceiver.class);

    @Test
    public void testChangeAdopt() {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler,
                taskStatusMachine, 0L, taskLogSaver, taskReportReceiver);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.READY)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.CREATED);
        verify(swTaskScheduler).schedule(List.of(task), taskReportReceiver);
        verify(swTaskScheduler, times(0)).stop(List.of(task));
    }

    @Test
    public void testChangeStopSchedule() {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler,
                taskStatusMachine, 100L, taskLogSaver, taskReportReceiver);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.PAUSED)
                .startTime(System.currentTimeMillis())
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(swTaskScheduler).stop(List.of(task));
        task.updateStatus(TaskStatus.CANCELED);
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(swTaskScheduler, times(0)).schedule(List.of(task), taskReportReceiver);
        // canceled do not trigger schedule
        verify(swTaskScheduler).stop(List.of(task));
    }

    @Test
    public void testDelayStopSchedule() throws InterruptedException {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler,
                taskStatusMachine, 1L, taskLogSaver, taskReportReceiver);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.FAIL)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        long current = System.currentTimeMillis() - 1000 * 60L + 1000; // +1s prevent immediately deletion

        Instant instant = Instant.ofEpochMilli(current);
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now).thenReturn(instant);
            taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.RUNNING);
            task.updateStatus(TaskStatus.SUCCESS);
            taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.RUNNING);
            taskWatcherForSchedule.processTaskDeletion();
            verify(swTaskScheduler, times(0)).stop(List.of(task));
            Thread.sleep(2000);
            taskWatcherForSchedule.processTaskDeletion();
            verify(swTaskScheduler, times(1)).stop(List.of(task, task));
        }
    }

    @Test
    public void testChangeIgnore() {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler,
                taskStatusMachine, 0L, taskLogSaver, taskReportReceiver);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.RUNNING)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.SUCCESS);

        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.FAIL);
        verify(swTaskScheduler, times(0)).schedule(List.of(task), taskReportReceiver);
        verify(swTaskScheduler, times(0)).stop(List.of(task));
    }

    @Test
    public void testFailBeforeStart() {
        SwTaskScheduler swTaskScheduler = mock(SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler,
                taskStatusMachine, 10L, taskLogSaver, taskReportReceiver);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.FAIL)
                .startTime(null)
                .finishTime(null)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        assertFalse(taskWatcherForSchedule.hasTaskToDelete());
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(swTaskScheduler, times(0)).schedule(List.of(task), taskReportReceiver);
        // add to delay queue so the invoked times is 0
        assertTrue(taskWatcherForSchedule.hasTaskToDelete());
        verify(swTaskScheduler, times(0)).stop(List.of(task));
    }

    @Test
    public void testCancelling() {
        SwTaskScheduler swTaskScheduler = mock(SwTaskScheduler.class);
        var taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler, taskStatusMachine, 0L, taskLogSaver,
                taskReportReceiver);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.CANCELLING)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.RUNNING);
        verify(swTaskScheduler, times(0)).schedule(List.of(task), taskReportReceiver);
        verify(swTaskScheduler, times(1)).stop(List.of(task));
    }
}
