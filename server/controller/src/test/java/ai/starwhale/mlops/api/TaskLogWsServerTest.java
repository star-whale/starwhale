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

package ai.starwhale.mlops.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.k8s.log.TaskLogK8sStreamingCollector;
import ai.starwhale.mlops.schedule.log.TaskLogCollectorFactory;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskLogWsServerTest {

    private TaskLogK8sStreamingCollector logK8sCollector;
    private IdConverter idConvertor;
    private Session session;

    @BeforeEach
    public void setup() {
        idConvertor = mock(IdConverter.class);
        session = mock(Session.class);
        logK8sCollector = mock(TaskLogK8sStreamingCollector.class);
    }

    @Test
    public void testOpen() throws IOException, ApiException, InterruptedException {
        var server = new TaskLogWsServer();
        server.setIdConvertor(idConvertor);
        TaskLogCollectorFactory logCollectorFactory = mock(TaskLogCollectorFactory.class);
        when(logCollectorFactory.streamingCollector(any())).thenReturn(logK8sCollector);
        server.setTaskLogCollectorFactory(logCollectorFactory);

        final Long taskId = 1L;
        when(session.getId()).thenReturn("1");
        when(idConvertor.revert(any())).thenReturn(taskId);
        when(logK8sCollector.readLine(any())).thenReturn("foo");
        server.onOpen(session, "1");
        verify(logCollectorFactory).streamingCollector(Task.builder().id(taskId).step(new Step()).build());
        TimeUnit.MILLISECONDS.sleep(500);
        verify(logK8sCollector).readLine(any());
    }
}
