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

import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
//TODO remove this class when dataset build is a job
public class CancellableJobLogK8sCollectorFactory {

    private final K8sClient k8sClient;

    public CancellableJobLogK8sCollectorFactory(K8sClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    public TaskLogStreamingCollector make(String jobName) throws IOException, ApiException {
        return new TaskLogK8sStreamingCollector(this.k8sClient, jobName);
    }
}
