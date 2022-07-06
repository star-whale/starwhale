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

package ai.starwhale.mlops.agent.task.log;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.api.protocol.report.req.TaskLog;
import ai.starwhale.mlops.api.protocol.report.resp.LogReader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import java.util.*;

public class LogRecorder {
    private final LoggerContext loggerContext;
    private final List<Appender> appenderList = new ArrayList<>();

    private Reader realtimeReader;

    public LogRecorder(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    public void register(Appender appender) {
        this.appenderList.add(appender);
    }

    public void unRegister(Appender appender) {
        this.appenderList.remove(appender);
    }

    public void info(String loggerName, String message, Object[] argArray, InferenceTask task) {
        log(loggerName, message, argArray, Level.INFO, null, task);
    }
    public void error(String loggerName, String message, Object[] argArray, Throwable throwable, InferenceTask task) {
        log(loggerName, message, argArray, Level.ERROR, throwable, task);
    }

    public void log(String loggerName, String message, Object[] argArray, Level level, Throwable throwable, InferenceTask task) {
        LoggingEvent loggingEvent = new LoggingEvent(this.getClass().getName(), loggerName, loggerContext, level, message, throwable, argArray);
        for (Appender appender : appenderList) {
            appender.append(task, loggingEvent);
        }
    }

    private Set<LogReader> readers = new HashSet<>();

    public void batchSubscribe(List<LogReader> logReaders) {
        Map<String, LogReader> newReaders = new HashMap<>();
        // subscribe
        for (LogReader newReader : logReaders) {
            newReaders.put(newReader.getReaderId(), newReader);
            realtimeReader.subscribe(newReader.getTaskId(), newReader.getReaderId());
        }
        // unsubscribe
        for (LogReader oldReader : readers) {
            if (!newReaders.containsKey(oldReader.getReaderId())) {
                realtimeReader.unSubscribe(oldReader.getTaskId(), oldReader.getReaderId());
            }
        }
        // renew
        readers = Set.copyOf(logReaders);
    }

    public List<TaskLog> generateLogs(Long taskId) {
        List<TaskLog> taskLogs = new ArrayList<>();
        Map<String, String> logs = realtimeReader.read(taskId);

        logs.forEach((readerId, log) -> {
            taskLogs.add(TaskLog.builder()
                    .readerId(readerId)
                    .log(log)
                    .build());
        });
        return taskLogs;
    }

    public void registerRealtimeReader(Reader realtimeReader) {
        this.realtimeReader = realtimeReader;
    }

    public Reader getRealtimeReader() {
        return realtimeReader;
    }
}
