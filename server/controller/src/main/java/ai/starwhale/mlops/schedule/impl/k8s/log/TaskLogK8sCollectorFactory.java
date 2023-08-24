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
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.log.TaskLogCollectorFactory;
import ai.starwhale.mlops.schedule.log.TaskLogOfflineCollector;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskLogK8sCollectorFactory implements TaskLogCollectorFactory {

    final K8sClient k8sClient;

    final K8sJobTemplate k8sJobTemplate;

    public TaskLogK8sCollectorFactory(K8sClient k8sClient, K8sJobTemplate k8sJobTemplate) {
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
    }


    @Override
    public TaskLogOfflineCollector offlineCollector(Task task) throws StarwhaleException {
        return new TaskLogOfflineCollectorK8s(k8sClient, k8sJobTemplate.getJobContainerNames(
                k8sJobTemplate.loadJobTemplate()), task);
    }

    @Override
    public TaskLogStreamingCollector streamingCollector(Task task) throws StarwhaleException {
        try {
            return new TaskLogK8sStreamingCollector(this.k8sClient, String.valueOf(task.getId()));
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.NETWORK,
                    MessageFormat.format("read k8s api exception {0}", e.getMessage()),
                    e);
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.INFRA,
                    MessageFormat.format("k8s api exception {0}", e.getResponseBody()),
                    e);
        }
    }
}
