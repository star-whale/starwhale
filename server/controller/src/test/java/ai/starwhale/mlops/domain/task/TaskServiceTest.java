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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.converter.TaskConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.github.pagehelper.PageInfo;
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

    JobManager jobManager;

    SystemSettingService systemSettingService;

    @BeforeEach
    public void setup() {
        taskConvertor = new TaskConverter(new IdConverter());
        taskMapper = mock(TaskMapper.class);
        storageAccessService = mock(StorageAccessService.class);
        jobManager = mock(JobManager.class);
        systemSettingService = mock(SystemSettingService.class);
        when(systemSettingService.queryResourcePool(anyString())).thenReturn(ResourcePool.defaults());
        taskService = new TaskService(taskConvertor, taskMapper, storageAccessService, jobManager,
                systemSettingService);
    }

    @Test
    public void testListTaskWithResourcePool() {
        when(jobManager.getJobId(anyString())).thenReturn(1L);
        when(jobManager.findJob(any())).thenReturn(JobEntity.builder().resourcePool("a").build());
        var startedTime = new Date();
        when(taskMapper.listTasks(1L)).thenReturn(
                List.of(TaskEntity.builder().id(1L).startedTime(startedTime).taskUuid("uuid1")
                                .taskStatus(
                                        TaskStatus.RUNNING).build(),
                        TaskEntity.builder().id(2L).startedTime(startedTime).taskUuid("uuid2")
                                .taskStatus(
                                        TaskStatus.SUCCESS).build()));
        PageInfo<TaskVo> taskVoPageInfo = taskService.listTasks("",
                PageParams.builder().pageNum(0).pageSize(3).build());
        Assertions.assertEquals(1, taskVoPageInfo.getPages());
        Assertions.assertEquals(2, taskVoPageInfo.getSize());
        Assertions.assertEquals(2, taskVoPageInfo.getList().size());
        assertThat(taskVoPageInfo.getList(), containsInAnyOrder(
                TaskVo.builder().id("1").createdTime(startedTime.getTime()).uuid("uuid1")
                        .taskStatus(TaskStatus.RUNNING).resourcePool(ResourcePool.DEFAULT_NAME).build(),
                TaskVo.builder().id("2").createdTime(startedTime.getTime()).uuid("uuid2")
                        .taskStatus(TaskStatus.SUCCESS).resourcePool(ResourcePool.DEFAULT_NAME).build()));


    }

    @Test
    public void testListTaskWithoutResourcePool() {
        when(jobManager.getJobId(anyString())).thenReturn(1L);
        when(jobManager.findJob(any())).thenReturn(JobEntity.builder().resourcePool("rp").build());
        var startedTime = new Date();
        when(taskMapper.listTasks(1L)).thenReturn(
                List.of(TaskEntity.builder().id(1L).startedTime(startedTime).taskUuid("uuid1")
                                .taskStatus(
                                        TaskStatus.RUNNING).build(),
                        TaskEntity.builder().id(2L).startedTime(startedTime).taskUuid("uuid2")
                                .taskStatus(
                                        TaskStatus.SUCCESS).build()));
        PageInfo<TaskVo> taskVoPageInfo = taskService.listTasks("",
                PageParams.builder().pageNum(0).pageSize(3).build());
        Assertions.assertEquals(1, taskVoPageInfo.getPages());
        Assertions.assertEquals(2, taskVoPageInfo.getSize());
        Assertions.assertEquals(2, taskVoPageInfo.getList().size());
        assertThat(taskVoPageInfo.getList(), containsInAnyOrder(
                TaskVo.builder().id("1").createdTime(startedTime.getTime()).uuid("uuid1")
                        .taskStatus(TaskStatus.RUNNING).resourcePool(ResourcePool.DEFAULT_NAME).build(),
                TaskVo.builder().id("2").createdTime(startedTime.getTime()).uuid("uuid2")
                        .taskStatus(TaskStatus.SUCCESS).resourcePool(ResourcePool.DEFAULT_NAME).build()));


    }

}
