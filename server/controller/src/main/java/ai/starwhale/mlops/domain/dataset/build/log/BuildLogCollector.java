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

package ai.starwhale.mlops.domain.dataset.build.log;

import ai.starwhale.mlops.domain.dataset.build.mapper.BuildRecordMapper;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Slf4j
@Component
public class BuildLogCollector {

    final String buildLogPath;

    final StorageAccessService storageService;
    final BuildRecordMapper buildRecordMapper;
    final K8sClient k8sClient;
    final List<String> containers;

    public BuildLogCollector(StorageAccessService storageService,
                             BuildRecordMapper buildRecordMapper,
                             K8sClient k8sClient,
                             K8sJobTemplate k8sJobTemplate,
                             @Value("${sw.dataset.build.log-path}") String buildLogPath) {
        this.storageService = storageService;
        this.buildRecordMapper = buildRecordMapper;
        this.k8sClient = k8sClient;
        this.containers = k8sJobTemplate.getJobContainerNames(
                k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_DATASET_BUILD));
        this.buildLogPath = buildLogPath;
    }

    public void collect(String jobName, Long id) throws StarwhaleException {
        log.debug("logging for dataset build {} begins...", id);
        try {
            V1Pod v1Pod = k8sClient.podOfJob(K8sClient.toV1LabelSelector(Map.of(
                    K8sJobTemplate.JOB_IDENTITY_LABEL, jobName)));
            if (null == v1Pod) {
                log.warn("pod not exists for dataset build {}", id);
                return;
            }
            String buildLog = k8sClient.logOfPod(v1Pod, containers);
            log.debug("logs for dataset build {} collected {} ...", id,
                    StringUtils.hasText(buildLog) ? buildLog.substring(0, Math.min(buildLog.length() - 1, 100)) : "");
            var record = buildRecordMapper.selectById(id);
            if (null == record) {
                log.warn("record not exists for dataset build {}", id);
                return;
            }
            String logPath = resolveLogPath(String.format("%s-%s", record.getDatasetName(), record.getId()));
            log.debug("putting log to storage at path {}", logPath);
            storageService.put(logPath, buildLog.getBytes(StandardCharsets.UTF_8));
            buildRecordMapper.updateLogPath(id, logPath);
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.INFRA,
                    MessageFormat.format("k8s api exception {0}", e.getResponseBody()), e);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "uploading log failed", e);
        }
    }

    private String resolveLogPath(String logName) {
        return String.format("%s/%s.log", buildLogPath, logName);
    }
}
