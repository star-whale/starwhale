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

import ai.starwhale.mlops.domain.run.bo.Run;
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
public class RunLogSaver {

    final RunLogCollectorFactory runLogCollectorFactory;

    final StorageAccessService storageService;

    public RunLogSaver(RunLogCollectorFactory runLogCollectorFactory, StorageAccessService storageService) {
        this.runLogCollectorFactory = runLogCollectorFactory;
        this.storageService = storageService;
    }

    public void saveLog(Run run) throws StarwhaleException {
        log.debug("logging for run {} begins...", run.getId());
        try {
            Tuple2<String, String> logInfo = runLogCollectorFactory.offlineCollector(run).collect();
            if (null == logInfo) {
                return;
            }
            String taskLog = logInfo._2();
            log.debug("logs for task {} collected {} ...", run.getId(),
                    StringUtils.hasText(taskLog) ? taskLog.substring(0, Math.min(taskLog.length() - 1, 100)) : "");
            String logPath = resolveLogPath(run, logInfo._1());
            log.debug("putting log to storage at path {}", logPath);
            storageService.put(logPath, taskLog.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "uploading log to storage failed", e);
        }
    }

    private String resolveLogPath(Run run, String logName) {
        return run.getLogDir() + "/" + logName;
    }

}
