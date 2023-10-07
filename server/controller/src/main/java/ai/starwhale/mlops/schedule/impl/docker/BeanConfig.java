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
import ai.starwhale.mlops.schedule.impl.docker.reporting.ContainerStatusExplainer;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerExecutorReporter;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration("dockerSchedulerBeanConfiguration")
@ConditionalOnProperty(value = "sw.scheduler.impl", havingValue = "docker")
public class BeanConfig {

    @Bean
    public DockerClientFinder dockerClientFinder() {
        return new DockerClientFinderSimpleImpl();
    }

    @Bean
    public ContainerRunMapper containerTaskMapper(DockerClientFinder dockerClientFinder) {
        return new ContainerRunMapper(dockerClientFinder);
    }

    @Bean
    public DockerExecutorReporter runReporter(
            @Lazy RunReportReceiver runReportReceiver,
            SystemSettingService systemSettingService,
            DockerClientFinder dockerClientFinder,
            ContainerStatusExplainer containerStatusExplainer,
            ContainerRunMapper containerRunMapper
    ) {
        return new DockerExecutorReporter(
                runReportReceiver,
                systemSettingService,
                dockerClientFinder,
                containerRunMapper,
                containerStatusExplainer
        );
    }

    @Bean
    public ContainerStatusExplainer containerStatusExplainer() {
        return new ContainerStatusExplainer();
    }

}
