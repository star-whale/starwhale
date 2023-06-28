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

package ai.starwhale.mlops.domain.job.step.task.log;

import ai.starwhale.mlops.domain.job.step.task.schedule.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class CancellableTaskLogK8sCollectorFactory {
    private final K8sClient k8sClient;

    public CancellableTaskLogK8sCollectorFactory(K8sClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    public CancellableTaskLogCollector make(Long taskId) throws IOException, ApiException {
        return new CancellableTaskLogK8sCollector(this.k8sClient, taskId);
    }
}
