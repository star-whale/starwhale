/**
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

package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.LogRecorder;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ai.starwhale.mlops.api.protocol.report.resp.LogReader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

@SpringBootTest(
        classes = StarWhaleAgentTestApplication.class)
@TestPropertySource(
        properties = {
                "sw.agent.task.rebuild.enabled=false",
                "sw.agent.task.scheduler.enabled=false",
                "sw.agent.node.sourcePool.init.enabled=false"
        }
)
public class LogRecorderTest {
    @Autowired
    private LogRecorder logRecorder;

    @MockBean
    private ContainerClient containerClient;
    @MockBean
    private TaskPersistence taskPersistence;

    //@Test
    public void test() {
        logRecorder.addRecords(List.of(
                LogReader.builder().readerId("r-1").taskId(1L).build(),
                LogReader.builder().readerId("r-2").taskId(2L).build()
        ));

        Mockito.when(taskPersistence.getActiveTaskById(1L)).thenReturn(
                Optional.of(InferenceTask.builder().id(1L).containerId("c-1").build())
        );
        Mockito.when(taskPersistence.getActiveTaskById(2L)).thenReturn(
                Optional.of(InferenceTask.builder().id(2L).containerId("c-2").build())
        );

        logRecorder.waitQueueScheduler();
    }
}
