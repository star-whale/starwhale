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
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.status.StepStatusMachine;
import ai.starwhale.mlops.domain.job.step.trigger.StepTrigger;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForJobStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * test for {@link TaskWatcherForJobStatus}
 */
public class TestTaskWatcherForJobStatus {

    @Test
    public void testSuccess() {

        JobMockHolder jobMockHolder = new JobMockHolder();
        Job job = jobMockHolder.mockJob();
        List<Task> tasks = job.getCurrentStep().getTasks();
        tasks.get(0).updateStatus(TaskStatus.SUCCESS);
        Task task = tasks.get(1);
        StepTrigger stepTriggerContext = mock(StepTrigger.class);

        StepMapper stepMapper = mock(StepMapper.class);
        JobUpdateHelper jobUpdateHelper = mock(JobUpdateHelper.class);
        LocalDateTimeConvertor localDateTimeConvertor = mock(LocalDateTimeConvertor.class);

        TaskWatcherForJobStatus taskWatcherForJobStatus = new TaskWatcherForJobStatus(new StepHelper(),
                new StepStatusMachine(),
                stepMapper, stepTriggerContext, jobUpdateHelper, localDateTimeConvertor);

        task.updateStatus(TaskStatus.SUCCESS);
        taskWatcherForJobStatus.onTaskStatusChange(task, TaskStatus.RUNNING);

        Step step = task.getStep();
        verify(stepMapper).updateStatus(List.of(step.getId()), StepStatus.SUCCESS);
        verify(stepMapper).updateFinishedTime(step.getId(), localDateTimeConvertor.revert(System.currentTimeMillis()));
        verify(stepTriggerContext).triggerNextStep(step);
        verify(jobUpdateHelper).updateJob(step.getJob());

    }

}
