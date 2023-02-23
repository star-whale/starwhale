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

package ai.starwhale.mlops.reporting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * test for {@link SimpleTaskStatusReceiver}
 */
public class TaskStatusReceiverImpTest {

    SimpleTaskStatusReceiver taskStatusReceiver;
    HotJobHolder jobHolder;

    TaskMapper taskMapper;

    @BeforeEach
    public void setup() {
        jobHolder = mock(HotJobHolder.class);
        taskMapper = mock(TaskMapper.class);
        taskStatusReceiver = new SimpleTaskStatusReceiver(jobHolder, taskMapper);
    }

    @Test
    public void testFreezeTask() {
        when(jobHolder.tasksOfIds(List.of(1L))).thenReturn(Collections.emptySet());
        taskStatusReceiver.receive(List.of(new ReportedTask(1L, TaskStatus.READY, null)));
        verify(taskMapper).updateTaskStatus(List.of(1L), TaskStatus.READY);
    }

    @Test
    public void testHotTask() {
        Task task = new Task();
        when(jobHolder.tasksOfIds(List.of(1L))).thenReturn(Set.of(task));
        taskStatusReceiver.receive(List.of(new ReportedTask(1L, TaskStatus.READY, null)));
        verify(taskMapper, times(0)).updateTaskStatus(List.of(1L), TaskStatus.READY);
        Assertions.assertEquals(TaskStatus.READY, task.getStatus());

    }
}
