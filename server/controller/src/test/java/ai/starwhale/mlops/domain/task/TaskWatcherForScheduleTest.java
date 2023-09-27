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
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForSchedule;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * test for {@link TaskWatcherForSchedule}
 */
public class TaskWatcherForScheduleTest {


    @Test
    public void testChangeAdopt() {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.READY)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.CREATED);
        verify(swTaskScheduler, times(0)).stop(task);
    }

    @Test
    public void testChangeStopSchedule() {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.PAUSED)
                .startTime(System.currentTimeMillis())
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(swTaskScheduler).stop(task);
        task.updateStatus(TaskStatus.CANCELED);
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(swTaskScheduler, times(0)).schedule(task);
        // canceled do not trigger schedule
        verify(swTaskScheduler).stop(task);
    }

    @Test
    public void testChangeIgnore() {
        SwTaskScheduler swTaskScheduler = mock(
                SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.RUNNING)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.SUCCESS);

        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.FAIL);
        verify(swTaskScheduler, times(0)).schedule(task);
        verify(swTaskScheduler, times(0)).stop(task);
    }

    @Test
    public void testFailBeforeStart() {
        SwTaskScheduler swTaskScheduler = mock(SwTaskScheduler.class);
        TaskWatcherForSchedule taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.FAIL)
                .startTime(null)
                .finishTime(null)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder()
                        .build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.READY);
        verify(swTaskScheduler, times(0)).schedule(task);
        // add to delay queue so the invoked times is 0
        verify(swTaskScheduler, times(0)).stop(task);
    }

    @Test
    public void testCancelling() {
        SwTaskScheduler swTaskScheduler = mock(SwTaskScheduler.class);
        var taskWatcherForSchedule = new TaskWatcherForSchedule(swTaskScheduler);
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.CANCELLING)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().build()).build()).build())
                .build();
        taskWatcherForSchedule.onTaskStatusChange(task, TaskStatus.RUNNING);
        verify(swTaskScheduler, times(0)).schedule(task);
        verify(swTaskScheduler, times(1)).stop(task);
    }
}
