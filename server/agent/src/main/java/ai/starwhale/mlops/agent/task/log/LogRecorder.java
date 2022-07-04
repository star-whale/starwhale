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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

public class LogRecorder {
    private final List<Appender> appenderList = new ArrayList<>();

    public void register(Appender appender) {
        this.appenderList.add(appender);
    }
    public void unRegister(Appender appender) {
        this.appenderList.remove(appender);
    }

    public void log(String fqcn, Logger logger, Level level, String message, Throwable throwable, Object[] argArray, InferenceTask task) {
        LoggingEvent loggingEvent = new LoggingEvent(fqcn, logger, level, message, throwable, argArray);
        for (Appender appender : appenderList) {
            appender.append(task, loggingEvent.getFormattedMessage());
        }
    }
}
