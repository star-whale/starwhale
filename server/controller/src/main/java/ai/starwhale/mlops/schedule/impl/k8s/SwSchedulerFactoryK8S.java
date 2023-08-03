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

package ai.starwhale.mlops.schedule.impl.k8s;

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.schedule.SwSchedulerAbstractFactory;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.impl.k8s.log.TaskLogK8sCollector;
import ai.starwhale.mlops.schedule.log.TaskLogCollector;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(value = "sw.scheduler", havingValue = "k8s")
public class SwSchedulerFactoryK8S implements SwSchedulerAbstractFactory {

    final K8sClient k8sClient;

    final RunTimeProperties runTimeProperties;

    final TaskTokenValidator taskTokenValidator;

    final K8sJobTemplate k8sJobTemplate;

    final String instanceUri;
    final int devPort;
    final int datasetLoadBatchSize;
    final String restartPolicy;
    final int backoffLimit;
    final StorageAccessService storageAccessService;
    final ThreadPoolTaskScheduler taskScheduler;

    final String ns;

    public SwSchedulerFactoryK8S(K8sClient k8sClient, RunTimeProperties runTimeProperties,
            TaskTokenValidator taskTokenValidator, K8sJobTemplate k8sJobTemplate,
            @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.task.dev-port}") int devPort,
            @Value("${sw.dataset.load.batch-size}") int datasetLoadBatchSize,
            @Value("${sw.infra.k8s.job.restart-policy}") String restartPolicy,
            @Value("${sw.infra.k8s.job.backoff-limit}") Integer backoffLimit,
            @Value("${sw.infra.k8s.name-space}") String ns,
            StorageAccessService storageAccessService,
            ThreadPoolTaskScheduler taskScheduler) {
        this.k8sClient = k8sClient;
        this.runTimeProperties = runTimeProperties;
        this.taskTokenValidator = taskTokenValidator;
        this.k8sJobTemplate = k8sJobTemplate;
        this.instanceUri = instanceUri;
        this.devPort = devPort;
        this.datasetLoadBatchSize = datasetLoadBatchSize;
        this.restartPolicy = restartPolicy;
        this.backoffLimit = backoffLimit;
        this.ns = ns;
        this.storageAccessService = storageAccessService;
        this.taskScheduler = taskScheduler;
    }

    @Bean
    @Override
    public SwTaskScheduler buildSwTaskScheduler() {
        return new K8SSwTaskScheduler(k8sClient, taskTokenValidator, runTimeProperties, k8sJobTemplate, instanceUri,
                devPort, datasetLoadBatchSize, restartPolicy, backoffLimit, storageAccessService, taskScheduler);
    }

    @Bean
    @Override
    public TaskLogCollector buildTaskLogCollector() {
        return new TaskLogK8sCollector(k8sClient, k8sJobTemplate);
    }

    @Bean
    public K8sClient k8sClient(ApiClient client,
            CoreV1Api coreV1Api,
            BatchV1Api batchV1Api,
            AppsV1Api appsV1Api,
            @Value("${sw.infra.k8s.name-space}") String ns,
            SharedInformerFactory informerFactory) {
        return new K8sClient(
                client,
                coreV1Api,
                batchV1Api,
                appsV1Api,
                ns,
                informerFactory
        );
    }

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
