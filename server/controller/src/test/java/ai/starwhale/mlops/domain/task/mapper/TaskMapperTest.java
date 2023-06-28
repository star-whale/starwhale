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

package ai.starwhale.mlops.domain.task.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.job.step.task.po.TaskEntity;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class TaskMapperTest extends MySqlContainerHolder {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private StepMapper stepMapper;


    @Test
    public void testAddAndGet() {
        String taskUuid = UUID.randomUUID().toString();
        TaskEntity task = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED).retryNum(0).taskUuid(taskUuid).stepId(1L).build();
        taskMapper.addTask(task);
        TaskEntity db2Memory = taskMapper.findTaskById(task.getId());
        Assertions.assertEquals(task, db2Memory);
    }

    @Test
    public void testListTasks() {
        long jobId = 132456L;
        StepEntity stp1 = StepEntity.builder().jobId(jobId).name("stp1").uuid(UUID.randomUUID().toString())
                .status(StepStatus.READY).concurrency(1)
                .taskNum(3).build();
        stepMapper.insert(stp1);
        TaskEntity task1 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(stp1.getId())
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(stp1.getId())
                .build();
        TaskEntity task3 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(stp1.getId() + 15365)
                .build();
        taskMapper.addAll(List.of(task1, task2, task3));
        List<TaskEntity> taskEntities = taskMapper.listTasks(jobId);
        Collections.sort(taskEntities, Comparator.comparing(TaskEntity::getId));
        Assertions.assertIterableEquals(taskEntities, List.of(task1, task2));
    }

    @Test
    public void testFindByStepId() {
        long jobId = 132453L;
        StepEntity stp1 = StepEntity.builder().jobId(jobId).name("stp1").uuid(UUID.randomUUID().toString())
                .status(StepStatus.READY).concurrency(1)
                .taskNum(3).build();
        stepMapper.insert(stp1);
        TaskEntity task1 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(stp1.getId())
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(stp1.getId())
                .build();
        TaskEntity task3 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(stp1.getId() + 1324)
                .build();
        taskMapper.addAll(List.of(task1, task2, task3));
        List<TaskEntity> taskEntities = taskMapper.findByStepId(stp1.getId());
        Collections.sort(taskEntities, Comparator.comparing(TaskEntity::getId));
        Assertions.assertIterableEquals(taskEntities, List.of(task1, task2));
    }

    @Test
    public void testUpdateTaskStatus() {
        TaskEntity task1 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        taskMapper.addTask(task1);
        taskMapper.addTask(task2);
        taskMapper.updateTaskStatus(List.of(task1.getId(), task2.getId()), TaskStatus.RUNNING);
        task1.setTaskStatus(TaskStatus.RUNNING);
        task2.setTaskStatus(TaskStatus.RUNNING);
        Assertions.assertEquals(task1, taskMapper.findTaskById(task1.getId()));
        Assertions.assertEquals(task2, taskMapper.findTaskById(task2.getId()));
    }

    @Test
    public void testFindTaskByStatus() {
        TaskEntity task1 = TaskEntity.builder()
                .taskStatus(TaskStatus.CANCELLING)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskStatus(TaskStatus.CANCELLING)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        TaskEntity task3 = TaskEntity.builder()
                .taskStatus(TaskStatus.CANCELED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        taskMapper.addAll(List.of(task1, task2, task3));
        List<TaskEntity> taskByStatus = taskMapper.findTaskByStatus(TaskStatus.CANCELLING);
        Collections.sort(taskByStatus, Comparator.comparing(TaskEntity::getId));
        Assertions.assertIterableEquals(taskByStatus, List.of(task1, task2));

        taskByStatus = taskMapper.findTaskByStatusIn(List.of(TaskStatus.CANCELLING, TaskStatus.CANCELED));
        Collections.sort(taskByStatus, Comparator.comparing(TaskEntity::getId));
        Assertions.assertIterableEquals(taskByStatus, List.of(task1, task2, task3));
    }

    @Test
    public void testUpdateTime() {
        TaskEntity task1 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        taskMapper.addTask(task1);
        taskMapper.addTask(task2);
        var now = new Date(System.currentTimeMillis());
        taskMapper.updateTaskFinishedTime(task1.getId(), now);
        taskMapper.updateTaskStartedTime(task2.getId(), now);
        final int milli500 = 5;
        Assertions.assertTrue(
                Math.abs(now.compareTo(taskMapper.findTaskById(task1.getId()).getFinishedTime())) <= milli500);
        Assertions.assertTrue(
                Math.abs(now.compareTo(taskMapper.findTaskById(task2.getId()).getStartedTime())) <= milli500);
    }

    @Test
    public void testUpdateRequest() {
        TaskEntity task1 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        taskMapper.addTask(task1);
        taskMapper.addTask(task2);
        String request = "request";
        taskMapper.updateTaskRequest(task1.getId(), request);
        Assertions.assertEquals(request, taskMapper.findTaskById(task1.getId()).getTaskRequest());
    }

    @Test
    public void testFailedReason() {
        TaskEntity task = TaskEntity.builder()
                .taskStatus(TaskStatus.CREATED)
                .retryNum(0)
                .taskUuid(UUID.randomUUID().toString())
                .stepId(1L)
                .build();
        taskMapper.addTask(task);
        String reason = "reason";
        taskMapper.updateFailedReason(task.getId(), reason);
        Assertions.assertEquals(reason, taskMapper.findTaskById(task.getId()).getFailedReason());
    }
}
