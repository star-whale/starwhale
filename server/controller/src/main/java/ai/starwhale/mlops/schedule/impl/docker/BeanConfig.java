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

package ai.starwhale.mlops.schedule.impl.docker;

import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.schedule.impl.docker.reporting.ContainerStatusExplainer;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerTaskReporter;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("dockerSchedulerBeanConfiguration")
@ConditionalOnProperty(value = "sw.scheduler.impl", havingValue = "docker")
public class BeanConfig {

    @Bean
    public DockerClientFinder dockerClientFinder() {
        return new DockerClientFinderSimpleImpl();
    }

    @Bean
    public ContainerTaskMapper containerTaskMapper(DockerClientFinder dockerClientFinder) {
        return new ContainerTaskMapper(dockerClientFinder);
    }

    @Bean
    public DockerTaskReporter taskReporter(
            TaskReportReceiver taskReportReceiver,
            SystemSettingService systemSettingService,
            DockerClientFinder dockerClientFinder,
            ContainerStatusExplainer containerStatusExplainer,
            TaskStatusMachine taskStatusMachine,
            ContainerTaskMapper containerTaskMapper
    ) {
        return new DockerTaskReporter(
                taskReportReceiver,
                systemSettingService,
                dockerClientFinder,
                containerTaskMapper,
                containerStatusExplainer,
                taskStatusMachine
        );
    }

    @Bean
    public ContainerStatusExplainer containerStatusExplainer() {
        return new ContainerStatusExplainer();
    }

}
