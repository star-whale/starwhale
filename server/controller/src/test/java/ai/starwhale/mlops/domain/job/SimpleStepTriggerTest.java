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

package ai.starwhale.mlops.domain.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.trigger.SimpleStepTrigger;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * a test for {@link SimpleStepTrigger}
 */
public class SimpleStepTriggerTest {

    @Test
    public void testEvalPplStepTrigger() throws IOException {

        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        List<String> pplResultPathA = List.of("a");
        List<String> pplResultPathB = List.of("b");
        when(storageAccessService.list("task_path_a/result")).thenReturn(pplResultPathA.stream());
        when(storageAccessService.list("task_path_b/result")).thenReturn(pplResultPathB.stream());
        TaskMapper taskMapper = mock(TaskMapper.class);
        SimpleStepTrigger evalPplStepTrigger = new SimpleStepTrigger(storageAccessService);

        Task task = mock(Task.class);
        long taskId = 123L;
        when(task.getId()).thenReturn(taskId);
        Step cmpStep = Step.builder().tasks(List.of(task)).build();
        Step pplStep = Step.builder()
                .tasks(List.of(
                        Task.builder().resultRootPath(new ResultPath("task_path_a")).build(),
                        Task.builder().resultRootPath(new ResultPath("task_path_b")).build()))
                .nextStep(cmpStep)
                .build();
        evalPplStepTrigger.triggerNextStep(pplStep);
        verify(task).updateStatus(TaskStatus.READY);

    }


}

