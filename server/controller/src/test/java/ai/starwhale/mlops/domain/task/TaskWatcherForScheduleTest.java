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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.schedule.TaskScheduler;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForSchedule;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * test for {@link TaskWatcherForSchedule}
 */
public class TaskWatcherForScheduleTest {

    @Test
    public void testChangeAdopt() {
        TaskScheduler taskScheduler = mock(
                TaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(taskScheduler, 0L);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.READY)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.CREATED);
        verify(taskScheduler).schedule(List.of(task));
        verify(taskScheduler, times(0)).stop(List.of(task));
    }

    @Test
    public void testChangeStopSchedule() {
        TaskScheduler taskScheduler = mock(
                TaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(taskScheduler, 100L);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.PAUSED)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        task.updateStatus(TaskStatus.CANCELED);
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(taskScheduler, times(0)).schedule(List.of(task));
        verify(taskScheduler, times(2)).stop(List.of(task));
    }

    @Test
    public void testDelayStopSchedule() throws InterruptedException {
        TaskScheduler taskScheduler = mock(
                TaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(taskScheduler, 1L);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.FAIL)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .startTime(System.currentTimeMillis() - 1000 * 60L + 1000) // +1s prevent immediately deletion
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.RUNNING);
        task.updateStatus(TaskStatus.SUCCESS);
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.RUNNING);
        taskWatcherForSchedule.processTaskDeletion();
        verify(taskScheduler, times(0)).stop(List.of(task));
        Thread.sleep(2000);
        taskWatcherForSchedule.processTaskDeletion();
        verify(taskScheduler, times(1)).stop(List.of(task, task));
    }

    @Test
    public void testChangeIgnore() {
        TaskScheduler taskScheduler = mock(
                TaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(taskScheduler, 0L);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.RUNNING)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.SUCCESS);

        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.FAIL);
        verify(taskScheduler, times(0)).schedule(List.of(task));
        verify(taskScheduler, times(0)).stop(List.of(task));
    }
}
