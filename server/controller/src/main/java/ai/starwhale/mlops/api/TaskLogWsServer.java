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

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.schedule.log.TaskLogCollector;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ServerEndpoint("/api/v1/log/online/{taskId}")
public class TaskLogWsServer {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static IdConverter idConvertor;

    final private TaskLogCollector taskLogCollector;

    private Session session;

    private String readerId;

    private Long id;

    private TaskLogStreamingCollector logCollector;

    public TaskLogWsServer(TaskLogCollector taskLogCollector) {
        this.taskLogCollector = taskLogCollector;
    }


    @Autowired
    public void setIdConvertor(IdConverter idConvertor) {
        TaskLogWsServer.idConvertor = idConvertor;
    }


    @OnOpen
    public void onOpen(Session session, @PathParam("taskId") String taskId) {
        this.session = session;
        this.readerId = session.getId();
        this.id = idConvertor.revert(taskId);
        try {
            logCollector = taskLogCollector.streaming(Task.builder().id(id).build());
        } catch (StarwhaleException e) {
            log.error("make k8s log collector failed", e);
        }
        log.info("Task log ws opened. reader={}, task={}", readerId, id);
        executorService.submit(() -> {
            String line;
            while (true) {
                try {
                    if ((line = logCollector.readLine()) == null) {
                        break;
                    }
                    sendMessage(line);
                } catch (IOException e) {
                    log.error("read k8s log failed", e);
                    break;
                }
            }
        });
    }

    @OnClose
    public void onClose() {
        cancelLogCollector();
        log.info("Task log ws closed. reader={}, task={}", readerId, id);
    }

    @OnMessage
    public void onMessage(String message, Session session) {

    }

    @OnError
    public void onError(Session session, Throwable error) {
        cancelLogCollector();
        log.error("Task log ws error: reader={}, task={}, message={}", readerId, id, error.getMessage());
    }

    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("ws send message", e);
        }
    }

    private void cancelLogCollector() {
        if (logCollector != null) {
            logCollector.cancel();
            logCollector = null;
        }
    }
}
