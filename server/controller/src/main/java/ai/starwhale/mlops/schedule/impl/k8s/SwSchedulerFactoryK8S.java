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

import ai.starwhale.mlops.schedule.SwSchedulerAbstractFactory;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.impl.k8s.log.TaskLogK8sCollectorFactory;
import ai.starwhale.mlops.schedule.log.TaskLogCollectorFactory;
import ai.starwhale.mlops.storage.StorageAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(value = "sw.scheduler", havingValue = "k8s")
public class SwSchedulerFactoryK8S implements SwSchedulerAbstractFactory {

    final K8sClient k8sClient;

    final K8sJobTemplate k8sJobTemplate;

    final TaskContainerSpecificationFinder taskContainerSpecificationFinder;

    final String restartPolicy;
    final int backoffLimit;
    final StorageAccessService storageAccessService;
    final ThreadPoolTaskScheduler cmdExecThreadPool;

    public SwSchedulerFactoryK8S(
            K8sClient k8sClient,
            K8sJobTemplate k8sJobTemplate,
            TaskContainerSpecificationFinder taskContainerSpecificationFinder,
            @Value("${sw.infra.k8s.job.restart-policy}") String restartPolicy,
            @Value("${sw.infra.k8s.job.backoff-limit}") Integer backoffLimit,
            StorageAccessService storageAccessService,
            ThreadPoolTaskScheduler cmdExecThreadPool
    ) {
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
        this.taskContainerSpecificationFinder = taskContainerSpecificationFinder;
        this.restartPolicy = restartPolicy;
        this.backoffLimit = backoffLimit;
        this.storageAccessService = storageAccessService;
        this.cmdExecThreadPool = cmdExecThreadPool;
    }

    @Bean
    @Override
    public SwTaskScheduler buildSwTaskScheduler() {
        return new K8sSwTaskScheduler(
                k8sClient,
                k8sJobTemplate,
                taskContainerSpecificationFinder,
                restartPolicy,
                backoffLimit,
                storageAccessService,
                cmdExecThreadPool);
    }

    @Bean
    @Override
    public TaskLogCollectorFactory buildTaskLogCollectorFactory() {
        return new TaskLogK8sCollectorFactory(k8sClient, k8sJobTemplate);
    }

}
