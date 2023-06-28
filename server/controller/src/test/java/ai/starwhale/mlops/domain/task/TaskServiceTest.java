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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import ai.starwhale.mlops.domain.job.step.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.job.step.task.converter.TaskConverter;
import ai.starwhale.mlops.domain.job.step.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.job.step.task.po.TaskEntity;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskServiceTest {

    TaskService taskService;
    TaskConverter taskConvertor;
    TaskMapper taskMapper;
    StorageAccessService storageAccessService;
    StepMapper stepMapper;

    @BeforeEach
    public void setup() {
        stepMapper = mock(StepMapper.class);
        when(stepMapper.findById(any())).thenReturn(new StepEntity() {
            {
                setName("ppl");
            }
        });
        taskConvertor = new TaskConverter(new IdConverter(), stepMapper, 8000, mock(WebServerInTask.class));
        taskMapper = mock(TaskMapper.class);
        storageAccessService = mock(StorageAccessService.class);
        taskService = new TaskService(taskConvertor, new TaskBoConverter(), taskMapper, storageAccessService);
    }

    @Test
    public void testListTaskWithJobResourcePool() throws IOException {
        var startedTime = new Date();
        var finishedTime = new Date();
        when(taskMapper.listTasks(1L)).thenReturn(
                List.of(TaskEntity.builder().id(1L).startedTime(startedTime).finishedTime(finishedTime)
                                .taskUuid("uuid1")
                                .taskStatus(
                                        TaskStatus.RUNNING).build(),
                        TaskEntity.builder().id(2L).startedTime(startedTime).finishedTime(finishedTime)
                                .taskUuid("uuid2")
                                .taskStatus(
                                        TaskStatus.SUCCESS).build()));
        when(stepMapper.findById(any())).thenReturn(new StepEntity() {
            {
                setName("ppl");
                // resource pool from step will equal to job's resource pool normally
                // we use this to test the priority of resource pool
                setPoolInfo(ResourcePool.builder().name("job from step").build().toJson());
            }
        });
        var tasks = taskService.listTasks(1L);
        Assertions.assertEquals(2, tasks.size());
        assertThat(tasks, containsInAnyOrder(
                TaskVo.builder().id("1")
                        .startedTime(startedTime.getTime())
                        .finishedTime(finishedTime.getTime())
                        .uuid("uuid1")
                        .resourcePool("job from step")
                        .taskStatus(TaskStatus.RUNNING).stepName("ppl").build(),
                TaskVo.builder().id("2")
                        .startedTime(startedTime.getTime())
                        .finishedTime(finishedTime.getTime())
                        .uuid("uuid2")
                        .resourcePool("job from step")
                        .taskStatus(TaskStatus.SUCCESS).stepName("ppl").build()));
    }

    @Test
    public void testGetTask() {
        var startedTime = new Date();
        var finishedTime = new Date();
        var task = TaskEntity.builder()
                .id(1L)
                .startedTime(startedTime)
                .finishedTime(finishedTime)
                .taskUuid("uuid1")
                .taskStatus(TaskStatus.RUNNING)
                .build();
        when(taskMapper.findTaskById(1L)).thenReturn(task);
        when(taskMapper.findTaskByUuid("uuid1")).thenReturn(task);

        var taskVo = TaskVo.builder()
                .id("1")
                .startedTime(startedTime.getTime())
                .finishedTime(finishedTime.getTime())
                .taskStatus(TaskStatus.RUNNING)
                .uuid("uuid1")
                .stepName("ppl")
                .resourcePool("")
                .build();
        assertThat("by id", taskService.getTask("1"), is(taskVo));
        assertThat("by uuid", taskService.getTask("uuid1"), is(taskVo));
    }
}
