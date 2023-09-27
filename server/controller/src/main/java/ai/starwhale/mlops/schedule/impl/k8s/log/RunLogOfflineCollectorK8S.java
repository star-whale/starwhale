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

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.log.RunLogOfflineCollector;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.vavr.Tuple2;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RunLogOfflineCollectorK8S implements RunLogOfflineCollector {

    final K8sClient k8sClient;

    final List<String> containers;

    final Run run;

    public RunLogOfflineCollectorK8S(K8sClient k8sClient, List<String> containers, Run run) {
        this.k8sClient = k8sClient;
        this.containers = containers;
        this.run = run;
    }

    @Override
    public Tuple2<String, String> collect() throws StarwhaleException {
        log.debug("logging for task {} begins...", run.getId());
        try {
            V1Pod v1Pod = k8sClient.podOfJob(K8sClient.toV1LabelSelector(Map.of(
                    K8sJobTemplate.JOB_IDENTITY_LABEL, run.getId().toString())));
            if (null == v1Pod) {
                log.error("pod not exists for task {}", run.getId());
                return null;
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

}
