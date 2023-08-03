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

package ai.starwhale.mlops.schedule.log;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.vavr.Tuple2;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TaskLogSaver {

    final TaskLogCollector logCollector;

    final StorageAccessService storageService;

    public TaskLogSaver(TaskLogCollector logCollector, StorageAccessService storageService) {
        this.logCollector = logCollector;
        this.storageService = storageService;
    }

    public void saveLog(Task task) throws StarwhaleException {
        log.debug("logging for task {} begins...", task.getId());
        try {
            Tuple2<String,String> logInfo = logCollector.collect(task);
            String taskLog = logInfo._2();
            log.debug("logs for task {} collected {} ...", task.getId(),
                    StringUtils.hasText(taskLog) ? taskLog.substring(0, Math.min(taskLog.length() - 1, 100)) : "");
            String logPath = resolveLogPath(task, logInfo._1());
            log.debug("putting log to storage at path {}", logPath);
            storageService.put(logPath, taskLog.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "uploading log to storage failed", e);
        }
    }

    private String resolveLogPath(Task task, String logName) {
        return task.getResultRootPath().logDir() + "/" + logName;
    }

}
