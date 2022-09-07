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

package ai.starwhale.mlops.domain.task.status.watchers.log;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import ai.starwhale.mlops.schedule.k8s.K8sJobTemplate;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Container;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TaskLogK8sCollector implements TaskLogCollector {


    final StorageAccessService storageService;

    final K8sClient k8sClient;

    final K8sJobTemplate k8sJobTemplate;

    public TaskLogK8sCollector(StorageAccessService storageService,
            K8sClient k8sClient, K8sJobTemplate k8sJobTemplate) {
        this.storageService = storageService;
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
    }

    @Override
    public void collect(Task task) throws StarwhaleException {
        String taskLog;
        log.debug("logging for task {} begins...", task.getId());
        try {
            taskLog = k8sClient.logOfJob(K8sClient.toV1LabelSelector(Map.of(
                            K8sJobTemplate.jobIdentityLabel, task.getId().toString())),
                    Stream.concat(k8sJobTemplate.getInitContainerTemplates().stream(),
                                    k8sJobTemplate.getContainersTemplates().stream())
                            .map(V1Container::getName)
                            .collect(Collectors.toList()));
            log.debug("logs for task {} is {}...", task.getId(),
                    StringUtils.hasText(taskLog) ? taskLog.substring(0, Math.min(taskLog.length() - 1, 100)) : "");
        } catch (ApiException e) {
            log.error("k8s api error ", e);
            throw new SwProcessException(ErrorType.INFRA).tip("k8s api exception" + e.getMessage());
        } catch (IOException e) {
            log.error("connection error ", e);
            throw new SwProcessException(ErrorType.NETWORK).tip("k8s connection exception" + e.getMessage());
        }
        try {
            String logPath = resolveLogPath(task);
            log.debug("putting log to storage at path {}", logPath);
            storageService.put(logPath, taskLog.getBytes(
                    StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("storage error ", e);
            throw new SwProcessException(ErrorType.STORAGE).tip("uploading log failed" + e.getMessage());
        }
    }

    private String resolveLogPath(Task task) {
        return task.getResultRootPath().logDir() + "/log";
    }
}
