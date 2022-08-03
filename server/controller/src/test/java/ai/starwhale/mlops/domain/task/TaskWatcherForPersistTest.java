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

import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForPersist;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * test for {@link TaskWatcherForPersist}
 */
public class TaskWatcherForPersistTest {

    @Test
    public void testStart(){
        TaskMapper taskMapper = mock(TaskMapper.class);
        LocalDateTimeConvertor localDateTimeConvertor = mock(LocalDateTimeConvertor.class);
        TaskWatcherForPersist taskWatcherForPersist = new TaskWatcherForPersist(new TaskStatusMachine(),taskMapper,localDateTimeConvertor);
        Task task = Task.builder()
            .id(1L)
            .uuid(UUID.randomUUID().toString())
            .status(TaskStatus.PREPARING).agent(
                Agent.builder().serialNumber("da").build())
            .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().deviceClass(
                Clazz.CPU).build()).build()).build())
            .build();
        taskWatcherForPersist.onTaskStatusChange(task,TaskStatus.READY);
        verify(taskMapper).updateTaskStartedTime(task.getId(),localDateTimeConvertor.revert(System.currentTimeMillis()));
        verify(taskMapper).updateTaskStatus(List.of(task.getId()),task.getStatus());
    }

    @Test
    public void testEnd(){
        TaskMapper taskMapper = mock(TaskMapper.class);
        LocalDateTimeConvertor localDateTimeConvertor = mock(LocalDateTimeConvertor.class);
        TaskWatcherForPersist taskWatcherForPersist = new TaskWatcherForPersist(new TaskStatusMachine(),taskMapper,localDateTimeConvertor);
        Task task = Task.builder()
            .id(1L)
            .uuid(UUID.randomUUID().toString())
            .status(TaskStatus.SUCCESS).agent(
                Agent.builder().serialNumber("da").build())
            .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().deviceClass(
                Clazz.CPU).build()).build()).build())
            .build();
        taskWatcherForPersist.onTaskStatusChange(task,TaskStatus.RUNNING);
        verify(taskMapper).updateTaskFinishedTime(task.getId(),localDateTimeConvertor.revert(System.currentTimeMillis()));
        verify(taskMapper).updateTaskStatus(List.of(task.getId()),task.getStatus());

    }

    @Test
    public void testNormal(){
        TaskMapper taskMapper = mock(TaskMapper.class);
        LocalDateTimeConvertor localDateTimeConvertor = mock(LocalDateTimeConvertor.class);
        TaskWatcherForPersist taskWatcherForPersist = new TaskWatcherForPersist(new TaskStatusMachine(),taskMapper,localDateTimeConvertor);
        Task task = Task.builder()
            .id(1L)
            .uuid(UUID.randomUUID().toString())
            .status(TaskStatus.RUNNING).agent(
                Agent.builder().serialNumber("da").build())
            .step(Step.builder().job(Job.builder().jobRuntime(JobRuntime.builder().deviceClass(
                Clazz.CPU).build()).build()).build())
            .build();
        taskWatcherForPersist.onTaskStatusChange(task,TaskStatus.PREPARING);
        verify(taskMapper,times(0)).updateTaskStartedTime(task.getId(),localDateTimeConvertor.revert(System.currentTimeMillis()));
        verify(taskMapper,times(0)).updateTaskFinishedTime(task.getId(),localDateTimeConvertor.revert(System.currentTimeMillis()));
        verify(taskMapper).updateTaskStatus(List.of(task.getId()),task.getStatus());
    }

}
