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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.job.JobOperator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepService;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForJobStatus;
import ai.starwhale.mlops.domain.job.step.trigger.StepTrigger;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * test for {@link TaskWatcherForJobStatus}
 */
public class TaskWatcherForJobStatusTest {

    @Test
    public void testJobRunning() {

        JobMockHolder jobMockHolder = new JobMockHolder();
        Job job = jobMockHolder.mockJob();
        List<Task> tasks = job.getCurrentStep().getTasks();
        tasks.get(0).updateStatus(TaskStatus.SUCCESS);
        Task task = tasks.get(1);
        StepTrigger stepTriggerContext = mock(StepTrigger.class);

        StepMapper stepMapper = mock(StepMapper.class);
        JobOperator jobOperator = mock(JobOperator.class);

        var taskWatcherForJobStatus = new TaskWatcherForJobStatus(
                new StepService(stepMapper, mock(StepConverter.class), mock(TaskService.class)),
                stepTriggerContext, jobOperator);

        task.updateStatus(TaskStatus.SUCCESS);
        taskWatcherForJobStatus.onTaskStatusChange(task, TaskStatus.RUNNING);

        Step step = task.getStep();
        verify(stepMapper).updateStatus(List.of(step.getId()), StepStatus.SUCCESS);
        verify(stepMapper).updateFinishedTime(eq(step.getId()), argThat(d -> d.getTime() > 0));
        verify(stepTriggerContext).triggerNextStep(step);
        verify(jobOperator).updateJob(step.getJob());

    }

    @Test
    public void testJobSuccess() {

        JobMockHolder jobMockHolder = new JobMockHolder();
        Job job = jobMockHolder.mockJob();
        List<Task> tasks = job.getCurrentStep().getTasks();
        tasks.get(0).updateStatus(TaskStatus.SUCCESS);
        Task task = tasks.get(1);
        StepTrigger stepTriggerContext = mock(StepTrigger.class);

        StepMapper stepMapper = mock(StepMapper.class);
        JobOperator jobOperator = mock(JobOperator.class);
        doAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setStatus(JobStatus.SUCCESS);
            return null;
        }).when(jobOperator).updateJob(job);

        var taskWatcherForJobStatus = new TaskWatcherForJobStatus(
                new StepService(stepMapper, mock(StepConverter.class), mock(TaskService.class)),
                stepTriggerContext, jobOperator);

        task.updateStatus(TaskStatus.SUCCESS);
        taskWatcherForJobStatus.onTaskStatusChange(task, TaskStatus.RUNNING);

        Step step = task.getStep();
        verify(stepMapper).updateStatus(List.of(step.getId()), StepStatus.SUCCESS);
        verify(stepMapper).updateFinishedTime(eq(step.getId()), argThat(d -> d.getTime() > 0));
        verify(stepTriggerContext, times(0)).triggerNextStep(step);
        verify(jobOperator).updateJob(step.getJob());

    }

    @Test
    public void testJobCanceled() {
        JobMockHolder jobMockHolder = new JobMockHolder();
        Job job = jobMockHolder.mockJob();
        List<Task> tasks = job.getCurrentStep().getTasks();
        tasks.get(0).updateStatus(TaskStatus.SUCCESS);
        Task task = tasks.get(1);
        StepTrigger stepTriggerContext = mock(StepTrigger.class);

        StepMapper stepMapper = mock(StepMapper.class);
        JobOperator jobOperator = mock(JobOperator.class);

        var taskWatcherForJobStatus = new TaskWatcherForJobStatus(
                new StepService(stepMapper, mock(StepConverter.class), mock(TaskService.class)),
                stepTriggerContext, jobOperator);

        task.updateStatus(TaskStatus.CANCELED);
        taskWatcherForJobStatus.onTaskStatusChange(task, TaskStatus.PREPARING);

        Step step = task.getStep();
        verify(stepMapper).updateStatus(List.of(step.getId()), StepStatus.CANCELED);
        verify(jobOperator).updateJob(step.getJob());
    }
}
