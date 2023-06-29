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

package ai.starwhale.mlops.domain.job;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import ai.starwhale.mlops.domain.job.step.task.WatchableTask;
import ai.starwhale.mlops.domain.job.step.task.WatchableTaskFactory;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.schedule.TaskScheduler;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * test for {@link JobScheduler}
 */
public class JobSchedulerTest {

    Job mockJob;

    HotJobHolder jobHolder;

    WatchableTaskFactory watchableTaskFactory;

    JobScheduler jobScheduler;

    TaskScheduler taskScheduler;

    @BeforeEach
    public void setUp() {
        mockJob = new JobMockHolder().mockJob();
        jobHolder = mock(HotJobHolder.class);
        watchableTaskFactory = mock(WatchableTaskFactory.class);
        taskScheduler = mock(TaskScheduler.class);
        jobScheduler = new JobScheduler(jobHolder, watchableTaskFactory, taskScheduler, mock(TaskService.class));
    }


    @Test
    public void testJobLoader() {
        Task failedTask = mock(Task.class);
        when(failedTask.getStatus()).thenReturn(TaskStatus.FAIL);
        Task readyTask = mock(Task.class);
        when(readyTask.getStatus()).thenReturn(TaskStatus.READY);
        when(watchableTaskFactory.wrapTasks(anyCollection())).thenReturn(List.of(failedTask, readyTask));
        jobScheduler.schedule(mockJob, false);
        verify(jobHolder, times(1)).add(mockJob);
        verify(watchableTaskFactory, times(mockJob.getSteps().size())).wrapTasks(anyCollection());
        verify(taskScheduler).schedule(Set.of(readyTask));
        verify(failedTask, times(0)).updateStatus(TaskStatus.READY);
    }

    @Test
    public void testJobLoaderResume() {
        WatchableTask failedTask = mock(WatchableTask.class);
        when(failedTask.getStatus()).thenReturn(TaskStatus.FAIL);
        when(failedTask.unwrap()).thenReturn(new Task());
        when(watchableTaskFactory.wrapTasks(anyCollection())).thenReturn(List.of(failedTask));
        jobScheduler.schedule(mockJob, true);
        verify(failedTask, times(mockJob.getSteps().size())).updateStatus(TaskStatus.READY);
        verify(jobHolder).add(mockJob);
    }
}