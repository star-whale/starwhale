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

package ai.starwhale.test.domain.task;

import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.bo.ppl.PPLRequest;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.test.ObjectMockHolder;
import ai.starwhale.test.domain.swds.SWDSIndexLoaderImplTest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for {@link TaskBoConverter}
 */
public class TaskBoConvertorTest {

    SWModelPackage swModelPackage = SWModelPackage.builder().build();

    @Test
    public void testTaskBoConverter() {
        TaskBoConverter taskBoConverter = ObjectMockHolder.taskBoConverter();

        Task t = Task.builder().id(1L).status(TaskStatus.CANCELLING).build();
        TaskCommand taskCommand = taskBoConverter.toTaskCommand(t);
        Assertions.assertEquals(CommandType.CANCEL, taskCommand.getCommandType());
        Assertions.assertTrue(t == taskCommand.getTask());

        Step step = Step.builder().job(Job.builder().swmp(swModelPackage).jobRuntime(JobRuntime.builder().deviceAmount(1).deviceClass(
            Clazz.CPU).name("name_swrt").storagePath("path_storage").version("version_swrt").build()).build()).build();
        TaskEntity pplTask = TaskEntity.builder()
            .id(1L)
            .agent(AgentEntity.builder().connectTime(LocalDateTime.now()).id(1L).serialNumber("serial1").build())
            .taskStatus(TaskStatus.RUNNING)
            .resultPath("path_task1")
            .startedTime(LocalDateTime.now())
            .taskRequest(SWDSIndexLoaderImplTest.INDEX_CONTENT)
            .taskUuid(UUID.randomUUID().toString())
            .taskType(TaskType.PPL).build();
        TaskEntity cmpTask = TaskEntity.builder()
            .id(2L)
            .agent(null)
            .taskStatus(TaskStatus.CREATED)
            .taskRequest("a\nb\nc")
            .resultPath("path_task2")
            .taskUuid(UUID.randomUUID().toString())
            .taskType(TaskType.CMP).build();
        Task task1 = taskBoConverter.transformTask(step, pplTask);
        Task task2 = taskBoConverter.transformTask(step, cmpTask);
        TaskTrigger taskTrigger1 = taskBoConverter.toTaskTrigger(task1);
        TaskTrigger taskTrigger2 = taskBoConverter.toTaskTrigger(task2);


        compareEntityAndTask(step,pplTask, task1);
        compareEntityAndTask(step,cmpTask, task2);

        compareTriggerAndTask(task1,taskTrigger1);
        compareTriggerAndTask(task2,taskTrigger2);

    }

    private void compareTriggerAndTask(Task task, TaskTrigger taskTrigger) {
        Assertions.assertEquals(task.getId(),taskTrigger.getId());
        Assertions.assertTrue(task.getResultRootPath() == taskTrigger.getResultPath());
        Assertions.assertEquals(task.getTaskType(),taskTrigger.getTaskType());
        Assertions.assertEquals(task.getStep().getJob().getJobRuntime().getDeviceClass(),taskTrigger.getDeviceClass());
        Assertions.assertTrue(swModelPackage == taskTrigger.getSwModelPackage());
        if(task.getTaskType() == TaskType.CMP){
            Assertions.assertTrue(taskTrigger.getCmpInputFilePaths().contains("a"));
            Assertions.assertTrue(taskTrigger.getCmpInputFilePaths().contains("b"));
            Assertions.assertTrue(taskTrigger.getCmpInputFilePaths().contains("c"));
            Assertions.assertEquals(1,taskTrigger.getDeviceAmount());
        }else {
            Assertions.assertEquals(task.getStep().getJob().getJobRuntime().getDeviceAmount(),taskTrigger.getDeviceAmount());

        }
    }

    private void compareEntityAndTask(Step step,TaskEntity entity, Task task) {
        Assertions.assertEquals(entity.getId(), task.getId());
        Assertions.assertTrue(step == task.getStep());
        Assertions.assertEquals(entity.getTaskStatus(), task.getStatus());
        Assertions.assertEquals(entity.getResultPath(), task.getResultRootPath().getRoot());
        Assertions.assertEquals(entity.getTaskUuid(), task.getUuid());
        Assertions.assertEquals(entity.getTaskType(), task.getTaskType());
        if(entity.getTaskType() == TaskType.PPL){
            Assertions.assertTrue(task.getTaskRequest() instanceof PPLRequest);
        }else {
            Assertions.assertTrue(task.getTaskRequest() instanceof CMPRequest);
        }

        AgentEntity agentEntity = entity.getAgent();
        if(null != agentEntity){
            Agent agent = task.getAgent();
            Assertions.assertNotNull(agent);
            Assertions.assertEquals(agentEntity.getId(), agent.getId());
            Assertions.assertEquals(agentEntity.getSerialNumber(),agent.getSerialNumber());
        }else {
            Assertions.assertNull(task.getAgent());
        }

    }
}
