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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusCalculator;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import ai.starwhale.mlops.domain.job.step.task.WatchableTask;
import ai.starwhale.mlops.domain.job.step.task.WatchableTaskFactory;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.schedule.TaskScheduler;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * test for {@link JobOperator}
 */
public class JobOperatorTest {

    Job mockJob;

    HotJobHolder jobHolder;

    JobDao jobDao;

    WatchableTaskFactory watchableTaskFactory;

    JobOperator jobOperator;

    TaskScheduler taskScheduler;

    @BeforeEach
    public void setUp() {
        mockJob = new JobMockHolder().mockJob();
        jobHolder = mock(HotJobHolder.class);
        jobDao = mock(JobDao.class);
        watchableTaskFactory = mock(WatchableTaskFactory.class);
        taskScheduler = mock(TaskScheduler.class);
        jobOperator = new JobOperator(jobHolder, jobDao, watchableTaskFactory, taskScheduler, mock(TaskService.class));
    }


    @Test
    public void testJobLoader() {
        Task failedTask = mock(Task.class);
        when(failedTask.getStatus()).thenReturn(TaskStatus.FAIL);
        Task readyTask = mock(Task.class);
        when(readyTask.getStatus()).thenReturn(TaskStatus.READY);
        when(watchableTaskFactory.wrapTasks(anyCollection())).thenReturn(List.of(failedTask, readyTask));
        jobOperator.addAndSchedule(mockJob, false);
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
        jobOperator.addAndSchedule(mockJob, true);
        verify(failedTask, times(mockJob.getSteps().size())).updateStatus(TaskStatus.READY);
        verify(jobHolder).add(mockJob);
    }


    @Test
    public void testSuccess() {
        JobStatus desiredStatus = JobStatus.SUCCESS;
        try (MockedStatic<JobStatusCalculator> statusCalculator = mockStatic(JobStatusCalculator.class)) {
            statusCalculator.when(
                    () -> JobStatusCalculator.desiredJobStatus(anyCollection())).thenReturn(desiredStatus);

            Job mockJob = new JobMockHolder().mockJob();
            mockJob.getSteps().parallelStream().forEach(step -> {
                step.getTasks().parallelStream().forEach(t -> {
                    t.updateStatus(TaskStatus.SUCCESS);
                });
            });

            jobOperator.updateJob(mockJob);
            Assertions.assertEquals(desiredStatus, mockJob.getStatus());
            verify(jobDao).updateJobStatus(mockJob.getId(), desiredStatus);
            verify(jobHolder).remove(mockJob.getId());
            verify(jobDao).updateJobFinishedTime(eq(mockJob.getId()),
                    argThat(d -> d.getTime() > 0), argThat(d -> d > 0));
        }
    }

    @Test
    public void testFail() throws InterruptedException {
        JobStatus desiredStatus = JobStatus.FAIL;
        try (MockedStatic<JobStatusCalculator> statusCalculator = mockStatic(JobStatusCalculator.class)) {
            statusCalculator.when(
                    () -> JobStatusCalculator.desiredJobStatus(anyCollection())).thenReturn(desiredStatus);
            Job mockJob = new JobMockHolder().mockJob();

            given(jobHolder.ofId(mockJob.getId())).willReturn(mockJob);

            Task luckTask = mockJob.getSteps().get(0).getTasks().get(0);
            luckTask.updateStatus(TaskStatus.RUNNING);

            jobOperator.updateJob(mockJob);
            Assertions.assertEquals(desiredStatus, mockJob.getStatus());
            verify(jobDao, times(1)).updateJobStatus(mockJob.getId(), desiredStatus);
            verify(jobHolder).remove(mockJob.getId());
            verify(jobDao).updateJobFinishedTime(eq(mockJob.getId()),
                    argThat(d -> d.getTime() > 0), argThat(d -> d > 0));
            Thread.sleep(100); // wait for async status update
            Assertions.assertEquals(TaskStatus.CANCELLING, luckTask.getStatus());
        }
    }

    @Test
    public void testCanceled() throws InterruptedException {
        JobStatus desiredStatus = JobStatus.CANCELED;
        try (MockedStatic<JobStatusCalculator> statusCalculator = mockStatic(JobStatusCalculator.class)) {
            statusCalculator.when(
                    () -> JobStatusCalculator.desiredJobStatus(anyCollection())).thenReturn(desiredStatus);
            Job mockJob = new JobMockHolder().mockJob();
            mockJob.setStatus(JobStatus.RUNNING);

            Thread.sleep(1L);
            jobOperator.updateJob(mockJob);
            Assertions.assertEquals(desiredStatus, mockJob.getStatus());
            verify(jobDao).updateJobStatus(mockJob.getId(), desiredStatus);
            verify(jobDao).updateJobFinishedTime(eq(mockJob.getId()),
                    argThat(d -> d.getTime() > 0), argThat(d -> d >= 0));
        }
    }

    @Test
    public void testRunning() {
        JobStatus desiredStatus = JobStatus.RUNNING;
        try (MockedStatic<JobStatusCalculator> statusCalculator = mockStatic(JobStatusCalculator.class)) {
            statusCalculator.when(
                    () -> JobStatusCalculator.desiredJobStatus(anyCollection())).thenReturn(desiredStatus);
            Job mockJob = new JobMockHolder().mockJob();
            mockJob.setStatus(JobStatus.READY);
            jobOperator.updateJob(mockJob);
            Assertions.assertEquals(JobStatus.RUNNING, mockJob.getStatus());
            verify(jobDao, times(1)).updateJobStatus(mockJob.getId(), JobStatus.RUNNING);
        }
    }

    @Test
    public void testCancel() {
        var desiredStatus = JobStatus.CANCELED;
        try (MockedStatic<JobStatusCalculator> statusCalculator = mockStatic(JobStatusCalculator.class)) {
            statusCalculator.when(
                    () -> JobStatusCalculator.desiredJobStatus(anyCollection())).thenReturn(desiredStatus);
            var mockJob = new JobMockHolder().mockJob();
            var step = Step.builder()
                    .status(StepStatus.CANCELED)
                    .build();
            mockJob.setSteps(List.of(step));

            mockJob.setStatus(JobStatus.RUNNING);
            jobOperator.updateJob(mockJob);
            Assertions.assertEquals(JobStatus.CANCELED, mockJob.getStatus());
            verify(jobDao, times(1)).updateJobStatus(mockJob.getId(), desiredStatus);
        }
    }
}
