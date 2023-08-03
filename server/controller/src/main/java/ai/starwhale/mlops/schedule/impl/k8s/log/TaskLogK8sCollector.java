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

package ai.starwhale.mlops.schedule.impl.k8s.log;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.log.TaskLogCollector;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.vavr.Tuple2;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
public class TaskLogK8sCollector implements TaskLogCollector {

    final K8sClient k8sClient;

    final List<String> containers;

    public TaskLogK8sCollector(K8sClient k8sClient, K8sJobTemplate k8sJobTemplate) {
        this.k8sClient = k8sClient;
        this.containers = k8sJobTemplate.getJobContainerNames(
                k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL));
    }

    @Override
    public Tuple2<String,String> collect(Task task) throws StarwhaleException {
        log.debug("logging for task {} begins...", task.getId());
        try {
            V1Pod v1Pod = k8sClient.podOfJob(K8sClient.toV1LabelSelector(Map.of(
                    K8sJobTemplate.JOB_IDENTITY_LABEL, task.getId().toString())));
            if (null == v1Pod) {
                log.error("pod not exists for task {}", task.getId());
                throw new SwValidationException(ValidSubject.TASK,"no log for this task found");
            }
            String logName = v1Pod.getMetadata().getName();
            String taskLog = k8sClient.logOfPod(v1Pod, containers);
            return new Tuple2<>(logName, taskLog);
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.INFRA,
                    MessageFormat.format("k8s api exception {}", e.getResponseBody()),
                    e);
        }
    }

    @Override
    public TaskLogStreamingCollector streaming(Task task) throws StarwhaleException {
        try {
            return new TaskLogK8SStreamingCollector(this.k8sClient, String.valueOf(task.getId()));
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.NETWORK,
                    MessageFormat.format("read k8s api exception {}", e.getMessage()),
                    e);
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.INFRA,
                    MessageFormat.format("k8s api exception {}", e.getResponseBody()),
                    e);
        }
    }
}
