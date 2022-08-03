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
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.log.TaskLogCollector;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForLogging;
import ai.starwhale.mlops.JobMockHolder;
import org.junit.jupiter.api.Test;

public class TaskWatcherForLoggingTest {

    @Test
    public void testFAIL(){
        TaskLogCollector taskLogCollector = mock(
            TaskLogCollector.class);
        TaskWatcherForLogging taskWatcherForLogging = new TaskWatcherForLogging(taskLogCollector,new TaskStatusMachine());
        Job mockJob = new JobMockHolder().mockJob();
        Task task = mockJob.getSteps().get(0).getTasks().get(0);
        task.updateStatus(TaskStatus.FAIL);
        taskWatcherForLogging.onTaskStatusChange(task,TaskStatus.RUNNING);
        verify(taskLogCollector).collect(task);
    }

    @Test
    public void testCANCELED(){
        TaskLogCollector taskLogCollector = mock(
            TaskLogCollector.class);
        TaskWatcherForLogging taskWatcherForLogging = new TaskWatcherForLogging(taskLogCollector,new TaskStatusMachine());
        Job mockJob = new JobMockHolder().mockJob();
        Task task = mockJob.getSteps().get(0).getTasks().get(0);

        task.updateStatus(TaskStatus.CANCELED);
        taskWatcherForLogging.onTaskStatusChange(task,TaskStatus.RUNNING);
        verify(taskLogCollector,times(0)).collect(task);

    }

    @Test
    public void testSUCCESS(){
        TaskLogCollector taskLogCollector = mock(
            TaskLogCollector.class);
        TaskWatcherForLogging taskWatcherForLogging = new TaskWatcherForLogging(taskLogCollector,new TaskStatusMachine());
        Job mockJob = new JobMockHolder().mockJob();
        Task task = mockJob.getSteps().get(0).getTasks().get(0);

        task.updateStatus(TaskStatus.SUCCESS);
        taskWatcherForLogging.onTaskStatusChange(task,TaskStatus.RUNNING);
        verify(taskLogCollector).collect(task);

    }

    @Test
    public void testRUNNING(){
        TaskLogCollector taskLogCollector = mock(
            TaskLogCollector.class);
        TaskWatcherForLogging taskWatcherForLogging = new TaskWatcherForLogging(taskLogCollector,new TaskStatusMachine());
        Job mockJob = new JobMockHolder().mockJob();
        Task task = mockJob.getSteps().get(0).getTasks().get(0);

        task.updateStatus(TaskStatus.RUNNING);
        taskWatcherForLogging.onTaskStatusChange(task,TaskStatus.READY);
        verify(taskLogCollector,times(0)).collect(task);
    }
}
