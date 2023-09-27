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
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.WatchableTask;
import ai.starwhale.mlops.domain.task.status.WatchableTaskFactory;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * test for {@link JobLoader}
 */
public class JobLoaderTest {

    Job mockJob;

    HotJobHolder jobHolder;

    WatchableTaskFactory watchableTaskFactory;

    JobLoader jobLoader;

    SwTaskScheduler swTaskScheduler;

    @BeforeEach
    public void setUp() {
        mockJob = new JobMockHolder().mockJob();
        jobHolder = mock(HotJobHolder.class);
        watchableTaskFactory = mock(WatchableTaskFactory.class);
        swTaskScheduler = mock(SwTaskScheduler.class);
        jobLoader = new JobLoader(jobHolder, watchableTaskFactory, swTaskScheduler);
    }


    @Test
    public void testJobLoader() {
        Task failedTask = mock(Task.class);
        when(failedTask.getStatus()).thenReturn(TaskStatus.FAIL);
        Task readyTask = mock(Task.class);
        when(readyTask.getStatus()).thenReturn(TaskStatus.READY);
        when(watchableTaskFactory.wrapTasks(anyCollection())).thenReturn(List.of(failedTask, readyTask));
        jobLoader.load(mockJob, false);
        verify(jobHolder, times(1)).adopt(mockJob);
        verify(watchableTaskFactory, times(mockJob.getSteps().size())).wrapTasks(anyCollection());
        verify(swTaskScheduler).schedule(readyTask);
        verify(failedTask, times(0)).updateStatus(TaskStatus.READY);
    }

    @Test
    public void testJobLoaderResume() {
        WatchableTask failedTask = mock(WatchableTask.class);
        when(failedTask.getStatus()).thenReturn(TaskStatus.FAIL);
        when(failedTask.unwrap()).thenReturn(new Task());
        when(watchableTaskFactory.wrapTasks(anyCollection())).thenReturn(List.of(failedTask));
        jobLoader.load(mockJob, true);
        verify(failedTask, times(mockJob.getSteps().size())).updateStatus(TaskStatus.READY);
        verify(jobHolder).adopt(mockJob);
    }
}
