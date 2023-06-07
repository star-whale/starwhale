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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.converter.TaskConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.github.pagehelper.PageInfo;
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

    JobDao jobDao;

    StepMapper stepMapper;

    @BeforeEach
    public void setup() {
        stepMapper = mock(StepMapper.class);
        when(stepMapper.findById(any())).thenReturn(new StepEntity() {
            {
                setName("ppl");
            }
        });
        taskConvertor = new TaskConverter(new IdConverter(), stepMapper, 8000);
        taskMapper = mock(TaskMapper.class);
        storageAccessService = mock(StorageAccessService.class);
        jobDao = mock(JobDao.class);
        taskService = new TaskService(taskConvertor, taskMapper, storageAccessService, jobDao);
    }

    @Test
    public void testListTaskWithJobResourcePool() {
        when(jobDao.findJob(any())).thenReturn(
                Job.builder().id(1L).resourcePool(ResourcePool.builder().name("a").build()).build());
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
        PageInfo<TaskVo> taskVoPageInfo = taskService.listTasks("1",
                PageParams.builder().pageNum(0).pageSize(3).build());
        Assertions.assertEquals(1, taskVoPageInfo.getPages());
        Assertions.assertEquals(2, taskVoPageInfo.getSize());
        Assertions.assertEquals(2, taskVoPageInfo.getList().size());
        assertThat(taskVoPageInfo.getList(), containsInAnyOrder(
                TaskVo.builder().id("1").createdTime(startedTime.getTime()).endTime(finishedTime.getTime())
                        .uuid("uuid1")
                        .taskStatus(TaskStatus.RUNNING).resourcePool("a").stepName("ppl").build(),
                TaskVo.builder().id("2").createdTime(startedTime.getTime()).endTime(finishedTime.getTime())
                        .uuid("uuid2")
                        .taskStatus(TaskStatus.SUCCESS).resourcePool("a").stepName("ppl").build()));


    }

    @Test
    public void testListTaskWithStepResourcePool() throws IOException {
        when(jobDao.findJob(any())).thenReturn(
                Job.builder().id(1L).resourcePool(ResourcePool.builder().name("pool from job").build()).build());
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
        PageInfo<TaskVo> taskVoPageInfo = taskService.listTasks("1",
                PageParams.builder().pageNum(0).pageSize(3).build());
        Assertions.assertEquals(1, taskVoPageInfo.getPages());
        Assertions.assertEquals(2, taskVoPageInfo.getSize());
        Assertions.assertEquals(2, taskVoPageInfo.getList().size());
        assertThat(taskVoPageInfo.getList(), containsInAnyOrder(
                TaskVo.builder().id("1").createdTime(startedTime.getTime()).endTime(finishedTime.getTime())
                        .uuid("uuid1")
                        .taskStatus(TaskStatus.RUNNING).resourcePool("job from step").stepName("ppl").build(),
                TaskVo.builder().id("2").createdTime(startedTime.getTime()).endTime(finishedTime.getTime())
                        .uuid("uuid2")
                        .taskStatus(TaskStatus.SUCCESS).resourcePool("job from step").stepName("ppl").build()));


    }

}
