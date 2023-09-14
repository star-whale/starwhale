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

import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForDataConsumption;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


/**
 * test for {@link TaskWatcherForDataConsumption}
 */
public class TaskWatcherForDataConsumptionTest {

    DataLoader dataLoader;
    TaskWatcherForDataConsumption taskWatcherForDataConsumption;

    @BeforeEach
    public void setup() {
        dataLoader = mock(DataLoader.class);
        taskWatcherForDataConsumption = new TaskWatcherForDataConsumption(new TaskStatusMachine(), dataLoader);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "RUNNING,FAIL,1",
            "RUNNING,CANCELED,1",
            "RUNNING,SUCCESS,0",
            "READY,RUNNING,0",
            "RUNNING,CANCELLING,0"
    })
    public void testTaskStatusChange(TaskStatus oldStatus, TaskStatus newStatus, int resetCount) {
        Task task = Task.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .status(oldStatus)
                .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().build()).build()).build())
                .startTime(7L)
                .build();
        task.updateStatus(newStatus);
        taskWatcherForDataConsumption.onTaskStatusChange(task, oldStatus);

        verify(dataLoader, times(resetCount)).resetUnProcessed(String.valueOf(task.getId()));
    }

}
