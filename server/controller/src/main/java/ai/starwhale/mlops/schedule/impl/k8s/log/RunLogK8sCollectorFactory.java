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
import ai.starwhale.mlops.schedule.log.RunLogCollectorFactory;
import ai.starwhale.mlops.schedule.log.RunLogOfflineCollector;
import ai.starwhale.mlops.schedule.log.RunLogStreamingCollector;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunLogK8sCollectorFactory implements RunLogCollectorFactory {

    final K8sClient k8sClient;

    final K8sJobTemplate k8sJobTemplate;

    public RunLogK8sCollectorFactory(K8sClient k8sClient, K8sJobTemplate k8sJobTemplate) {
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
    }


    @Override
    public RunLogOfflineCollector offlineCollector(Run run) throws StarwhaleException {
        return new RunLogOfflineCollectorK8S(k8sClient, k8sJobTemplate.getJobContainerNames(
                k8sJobTemplate.loadJobTemplate()), run);
    }

    @Override
    public RunLogStreamingCollector streamingCollector(Run run) throws StarwhaleException {
        try {
            return new RunLogK8sStreamingCollector(this.k8sClient, String.valueOf(run.getId()));
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
