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

import ai.starwhale.mlops.ObjectMockHolder;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.swds.SWDSIndexLoaderImplTest;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
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
            .taskRequest("{\"project\":\"starwhale\",\"index\":0,\"datasetUris\":[\"mnist/version/myztqzrtgm3tinrtmftdgyjzob2ggni\"],\"jobId\":\"3d32264ce5054fa69190167e15d6303d\",\"total\":1,\"stepName\":\"ppl\"}")
            .startedTime(LocalDateTime.now())
            .taskRequest(SWDSIndexLoaderImplTest.INDEX_CONTENT)
            .taskUuid(UUID.randomUUID().toString())
            .build();
        TaskEntity cmpTask = TaskEntity.builder()
            .id(2L)
            .agent(null)
            .taskStatus(TaskStatus.CREATED)
            .taskRequest("{\"project\":\"starwhale\",\"index\":0,\"datasetUris\":[\"mnist/version/myztqzrtgm3tinrtmftdgyjzob2ggni\"],\"jobId\":\"3d32264ce5054fa69190167e15d6303d\",\"total\":1,\"stepName\":\"cmp\"}")
            .taskUuid(UUID.randomUUID().toString())
            .build();
        Task task1 = taskBoConverter.transformTask(step, pplTask);
        Task task2 = taskBoConverter.transformTask(step, cmpTask);

        compareEntityAndTask(step,pplTask, task1);
        compareEntityAndTask(step,cmpTask, task2);
    }
    private void compareEntityAndTask(Step step,TaskEntity entity, Task task) {
        Assertions.assertEquals(entity.getId(), task.getId());
        Assertions.assertTrue(step == task.getStep());
        Assertions.assertEquals(entity.getTaskStatus(), task.getStatus());
        Assertions.assertEquals(entity.getTaskUuid(), task.getUuid());

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
