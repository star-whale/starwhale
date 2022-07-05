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
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ch.qos.logback.classic.PatternLayout;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class FileLog implements Appender {
    private final TaskPersistence taskPersistence;
    private final PatternLayout patternLayout;

    public FileLog(TaskPersistence taskPersistence, PatternLayout patternLayout) {
        this.taskPersistence = taskPersistence;
        this.patternLayout = patternLayout;
    }

    @Override
    public void append(InferenceTask task, LoggingEvent loggingEvent) {
        taskPersistence.recordLog(task, patternLayout.doLayout(loggingEvent));
    }

    @Override
    public void finishAppend(InferenceTask task) {
        try {
            taskPersistence.uploadLog(task);
        } catch (Exception e) {
            log.error("upload log for task {} error", task.getId(), e);
        }
    }

}
