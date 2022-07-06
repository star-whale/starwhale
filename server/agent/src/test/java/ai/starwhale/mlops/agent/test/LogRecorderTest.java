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

package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ai.starwhale.mlops.agent.task.log.LogRecorder;
import ai.starwhale.mlops.agent.task.log.Reader;
import ai.starwhale.mlops.api.protocol.report.resp.LogReader;
import org.junit.jupiter.api.Assertions;
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
    private TaskPersistence taskPersistence;

    @Test
    public void simpleTest() {
        logRecorder.batchSubscribe(List.of(
                LogReader.builder().readerId("r-1").taskId(1L).build(),
                LogReader.builder().readerId("r-2").taskId(2L).build()
        ));
        InferenceTask task1 = InferenceTask.builder().id(1L).containerId("c-1").build();
        Mockito.when(taskPersistence.getActiveTaskById(1L)).thenReturn(
                Optional.of(task1)
        );
        Mockito.when(taskPersistence.getActiveTaskById(2L)).thenReturn(
                Optional.of(InferenceTask.builder().id(2L).containerId("c-2").build())
        );

        logRecorder.info("test.logger", "i'm {}, doing test", new Object[]{"starwhale"}, task1);
        // "2022-07-06 10:43:37.779  INFO [,,] 19932 --- [           main] test.logger                              : i'm starwhale, doing test\n"
        Assertions.assertEquals(1, logRecorder.generateLogs(1L).size());

        // r-1 unsubscribe, r-3 subscribe
        logRecorder.batchSubscribe(List.of(
                LogReader.builder().readerId("r-2").taskId(2L).build(),
                LogReader.builder().readerId("r-3").taskId(3L).build(),
                LogReader.builder().readerId("r-4").taskId(4L).build()
        ));
        // r-1 was cleaned but logCache wasn't
        Assertions.assertEquals(0, logRecorder.generateLogs(1L).size());
        Assertions.assertEquals(3, logRecorder.getRealtimeReader().subscriberSize());
        // set task1's status = archived
        task1.setStatus(InferenceTaskStatus.ARCHIVED);
        logRecorder.info("test.logger", "i'm {}, archived! ", new Object[]{"starwhale"}, task1);
        // trigger clean
        this.logRecorder.getRealtimeReader().clean();
        // logCache was cleaned
        Assertions.assertEquals(0, logRecorder.getRealtimeReader().logSize());

        // new log
        InferenceTask task2 = InferenceTask.builder().id(2L).containerId("c-2").build();
        Mockito.when(taskPersistence.getActiveTaskById(2L)).thenReturn(
                Optional.of(task2)
        );
        InferenceTask task3 = InferenceTask.builder().id(3L).containerId("c-3").build();
        Mockito.when(taskPersistence.getActiveTaskById(3L)).thenReturn(
                Optional.of(task3)
        );
        InferenceTask task4 = InferenceTask.builder().id(4L).containerId("c-4").build();
        Mockito.when(taskPersistence.getActiveTaskById(4L)).thenReturn(
                Optional.of(task4)
        );
        logRecorder.info("test.logger", "i'm {}, doing test-2-1", new Object[]{"starwhale"}, task2);
        logRecorder.info("test.logger", "i'm {}, doing test-2-2", new Object[]{"starwhale"}, task2);
        logRecorder.info("test.logger", "i'm {}, doing test-3-1", new Object[]{"starwhale"}, task3);
        logRecorder.error("test.logger", "i'm {}, doing test-4-1", new Object[]{"starwhale"}, new RuntimeException("none!"), task4);

        // read logs
        Assertions.assertEquals(1, logRecorder.generateLogs(2L).size());
        Assertions.assertEquals(1, logRecorder.generateLogs(3L).size());
        Assertions.assertEquals(1, logRecorder.generateLogs(4L).size());
        // assert the newest offset
        Assertions.assertEquals(2, logRecorder.getRealtimeReader().offset(2L, "r-2"));
        Assertions.assertEquals(1, logRecorder.getRealtimeReader().offset(3L, "r-3"));
        Assertions.assertEquals(1, logRecorder.getRealtimeReader().offset(4L, "r-4"));
    }

}
