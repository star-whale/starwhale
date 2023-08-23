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
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for {@link TaskBoConverter}
 */
public class TaskBoConverterTest {

    Model model = Model.builder().build();

    @Test
    public void testTaskBoConverter() {
        TaskBoConverter taskBoConverter = ObjectMockHolder.taskBoConverter();

        Step step = Step.builder()
                .job(Job.builder()
                        .model(model)
                        .jobRuntime(JobRuntime.builder()
                                .name("name_swrt")
                                .version("version_swrt")
                                .build())
                        .build())
                .build();
        TaskEntity pplTask = TaskEntity.builder()
                .id(1L)
                .taskStatus(TaskStatus.RUNNING)
                .taskRequest(
                        "{\"project\":\"starwhale\",\"index\":0,\"datasetUris\":"
                                + "[\"mnist/version/myztqzrtgm3tinrtmftdgyjzob2ggni\"],"
                                + "\"jobId\":\"3d32264ce5054fa69190167e15d6303d\",\"total\":1,\"stepName\":\"ppl\"}")
                .startedTime(new Date())
                .taskUuid(UUID.randomUUID().toString())
                .build();
        TaskEntity cmpTask = TaskEntity.builder()
                .id(2L)
                .taskStatus(TaskStatus.CREATED)
                .taskRequest(
                        "{\"project\":\"starwhale\",\"index\":0,\"datasetUris\":"
                                + "[\"mnist/version/myztqzrtgm3tinrtmftdgyjzob2ggni\"],"
                                + "\"jobId\":\"3d32264ce5054fa69190167e15d6303d\",\"total\":1,\"stepName\":\"cmp\"}")
                .taskUuid(UUID.randomUUID().toString())
                .build();
        Task task1 = taskBoConverter.transformTask(step, pplTask);
        Task task2 = taskBoConverter.transformTask(step, cmpTask);

        compareEntityAndTask(step, pplTask, task1);
        compareEntityAndTask(step, cmpTask, task2);
    }

    private void compareEntityAndTask(Step step, TaskEntity entity, Task task) {
        Assertions.assertEquals(entity.getId(), task.getId());
        Assertions.assertTrue(step == task.getStep());
        Assertions.assertEquals(entity.getTaskStatus(), task.getStatus());
        Assertions.assertEquals(entity.getTaskUuid(), task.getUuid());


    }
}
