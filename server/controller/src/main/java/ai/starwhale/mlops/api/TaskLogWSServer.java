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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.TaskLog;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.LogReader;
import ai.starwhale.mlops.common.IDConvertor;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
//@ServerEndpoint("${sw.controller.apiPrefix}/log/online/{taskId}")
public class TaskLogWSServer {

    private static final ConcurrentHashMap<String, TaskLogWSServer> sockets = new ConcurrentHashMap<>();

    private static IDConvertor idConvertor;

    private Session session;

    private String readerId;

    private Long id;

    @Autowired
    public void setIdConvertor(IDConvertor idConvertor) {
        TaskLogWSServer.idConvertor = idConvertor;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("taskId") String taskId) {
        this.session = session;
        this.readerId = session.getId();
        this.id = idConvertor.revert(taskId);
        sockets.put(readerId, this);
        log.info("Task log ws opened. reader={}, task={}", readerId, id);
    }

    @OnClose
    public void onClose() {
        if(sockets.containsKey(readerId)) {
            sockets.remove(readerId);
            log.info("Task log ws closed. reader={}, task={}", readerId, id);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {

    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Task log ws error: reader={}, task={}, message={}", readerId, id, error.getMessage());
        log.error("", error);
    }

    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("ws send message", e);
        }
    }

    public static void sendMessage(String message, String readerId) {
        if(sockets.containsKey(readerId)) {
            sockets.get(readerId).sendMessage(message);
        } else {
            log.error("ws send message failed. Reader cannot be found! {}", readerId);
        }
    }

    public List<LogReader> getLogReaders() {
        List<LogReader> readers = Lists.newArrayList();
        for(TaskLogWSServer ws : sockets.values()) {
            readers.add(LogReader.builder().readerId(ws.readerId).taskId(ws.id).build());
        }

        return readers;
    }

    public void report(TaskReport taskReport) {
        String status = getLogStatus(taskReport.getStatus());
        if(taskReport.getReaderLogs() != null) {
            for (TaskLog readerLog : taskReport.getReaderLogs()) {
                report(readerLog, status);
            }
        }
    }

    private String getLogStatus(TaskStatusInterface taskStatus) {
        String status;
        switch(taskStatus) {
            case SUCCESS:
            case CANCELED:
            case FAIL:
                status = "FINISHED";
                break;
            default:
                status = "RUNNING";
        }
        return status;
    }

    private void report(TaskLog taskLog, String status) {
        String readerId = taskLog.getReaderId();
        if(sockets.containsKey(readerId)) {
            JSONObject json = JSONUtil.createObj();
            json.append("logIncrement", taskLog.getLog());
            json.append("status", status);
            //send log
            sendMessage(json.toString(), readerId);
        } else {
            log.error("ws send message failed. Reader cannot be found! {}", readerId);
        }
    }
}
