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

package ai.starwhale.mlops.domain.job.step.task.schedule.k8s;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class K8sClientConfig {

    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient apiClient = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(apiClient);
        return apiClient;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api();
    }

    @Bean
    public BatchV1Api batchV1Api(ApiClient apiClient) {
        return new BatchV1Api();
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api();
    }

    @Bean
    public SharedInformerFactory sharedInformerFactory(ApiClient apiClient) {
        return new SharedInformerFactory(apiClient);
    }
}
