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

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * test for {@link ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher}
 */
public class TaskWatcherTest {

    TaskStatusMachine taskStatusMachine = new TaskStatusMachine();

//    WatchableTaskFactory watchableTaskFactory = new WatchableTaskFactory(List.of(taskWatcherForCommandingAssurance,taskWatcherForJobStatus,taskWatcherForSchedule,taskWatcherForPersist),taskStatusMachine);


    @Test
    public void testTaskWatchable(){
        Task task = mockTask();
//        Task watchableTask = watchableTaskFactory.wrapTask(task);
//        watchableTask.updateStatus(TaskStatus.SUCCESS);
//        verify(mockedList).get(anyInt());
    }

    private Task mockTask() {
        Task task = Task.builder()
            .id(1L)
            .status(TaskStatus.RUNNING)
            .step(Step.builder().build())
            .resultRootPath(new ResultPath(""))
            .uuid(UUID.randomUUID().toString())
            .build();
        return task;
    }


}
