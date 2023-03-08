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
import io.kubernetes.client.openapi.models.V1Pod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TaskLogK8sCollector implements TaskLogCollector {


    final StorageAccessService storageService;

    final K8sClient k8sClient;

    final List<String> containers;

    public TaskLogK8sCollector(StorageAccessService storageService,
            K8sClient k8sClient, K8sJobTemplate k8sJobTemplate) {
        this.storageService = storageService;
        this.k8sClient = k8sClient;
        this.containers = k8sJobTemplate.getJobContainerNames(
                k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL));
    }

    @Override
    public void collect(Task task) throws StarwhaleException {
        log.debug("logging for task {} begins...", task.getId());
        try {
            V1Pod v1Pod = k8sClient.podOfJob(K8sClient.toV1LabelSelector(Map.of(
                    K8sJobTemplate.JOB_IDENTITY_LABEL, task.getId().toString())));
            if (null == v1Pod) {
                log.warn("pod not exists for task {}", task.getId());
                return;
            }
            String logName = v1Pod.getMetadata().getName() + System.currentTimeMillis() / 1000;
            String taskLog = k8sClient.logOfPod(v1Pod, containers);
            log.debug("logs for task {} collected {} ...", task.getId(),
                    StringUtils.hasText(taskLog) ? taskLog.substring(0, Math.min(taskLog.length() - 1, 100)) : "");
            String logPath = resolveLogPath(task, logName);
            log.debug("putting log to storage at path {}", logPath);
            storageService.put(logPath, taskLog.getBytes(
                    StandardCharsets.UTF_8));
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.INFRA,
                    MessageFormat.format("k8s api exception {}", e.getResponseBody()),
                    e);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "uploading log failed", e);
        }
    }

    private String resolveLogPath(Task task, String logName) {
        return task.getResultRootPath().logDir() + "/" + logName;
    }
}
