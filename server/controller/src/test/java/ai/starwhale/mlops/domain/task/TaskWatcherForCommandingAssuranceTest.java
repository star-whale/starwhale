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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForCommandingAssurance;
import ai.starwhale.mlops.schedule.CommandingTasksAssurance;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * test for {@link TaskWatcherForCommandingAssurance}
 */
public class TaskWatcherForCommandingAssuranceTest {

    @Test
    public void testOnChangeAssigning(){
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = mock(TaskBoConverter.class);
        TaskWatcherForCommandingAssurance taskWatcherForCommandingAssurance = new TaskWatcherForCommandingAssurance(
            commandingTasksAssurance, taskBoConverter);
        Task task = Task.builder().status(TaskStatus.ASSIGNING).agent(
            Agent.builder().serialNumber("da").build()).build();
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

    }

    @Test
    public void testOnChangeCancelling(){
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = mock(TaskBoConverter.class);
        TaskWatcherForCommandingAssurance taskWatcherForCommandingAssurance = new TaskWatcherForCommandingAssurance(
            commandingTasksAssurance, taskBoConverter);
        Task task = Task.builder().status(TaskStatus.CANCELLING).agent(
            Agent.builder().serialNumber("da").build()).build();
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.TO_CANCEL);
        verify(commandingTasksAssurance).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

    }

    @Test
    public void testOnChangeNotCommanding(){
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = mock(TaskBoConverter.class);
        TaskWatcherForCommandingAssurance taskWatcherForCommandingAssurance = new TaskWatcherForCommandingAssurance(
            commandingTasksAssurance, taskBoConverter);
        Task task = Task.builder().status(TaskStatus.PREPARING).agent(
            Agent.builder().serialNumber("da").build()).build();
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance,times(0)).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

        task.updateStatus(TaskStatus.RUNNING);
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance,times(0)).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

        task.updateStatus(TaskStatus.SUCCESS);
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance,times(0)).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

        task.updateStatus(TaskStatus.CANCELED);
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance,times(0)).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

        task.updateStatus(TaskStatus.FAIL);
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance,times(0)).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

        task.updateStatus(TaskStatus.PAUSED);
        taskWatcherForCommandingAssurance.onTaskStatusChange(task, TaskStatus.READY);
        verify(commandingTasksAssurance,times(0)).onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

    }


}
