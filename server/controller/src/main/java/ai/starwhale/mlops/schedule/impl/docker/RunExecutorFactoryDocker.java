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

import ai.starwhale.mlops.schedule.RunExecutorAbstractFactory;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.impl.docker.log.RunLogCollectorFactoryDocker;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerExecutorReporter;
import ai.starwhale.mlops.schedule.log.RunLogCollectorFactory;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(value = "sw.scheduler.impl", havingValue = "docker")
public class RunExecutorFactoryDocker implements RunExecutorAbstractFactory {

    final DockerClientFinder dockerClientFinder;

    final ContainerRunMapper containerRunMapper;

    final DockerExecutorReporter dockerExecutorReporter;

    final TaskContainerSpecificationFinder taskContainerSpecificationFinder;

    final String network;
    final String nodeIp;

    public RunExecutorFactoryDocker(DockerClientFinder dockerClientFinder, ContainerRunMapper containerRunMapper,
                                    DockerExecutorReporter dockerExecutorReporter,
                                    TaskContainerSpecificationFinder taskContainerSpecificationFinder,
                                    @Value("${sw.scheduler.docker.network}") String network,
                                    @Value("${sw.scheduler.docker.node-ip}") String nodeIp
    ) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerRunMapper = containerRunMapper;
        this.dockerExecutorReporter = dockerExecutorReporter;
        this.taskContainerSpecificationFinder = taskContainerSpecificationFinder;
        this.network = network;
        this.nodeIp = nodeIp;
    }

    @Bean
    @Override
    public RunExecutor buildRunExecutor() {
        return new RunExecutorDockerImpl(
                dockerClientFinder,
                new HostResourceConfigBuilder(),
                Executors.newCachedThreadPool(),
                containerRunMapper,
                nodeIp,
                network
        );
    }

    @Bean
    @Override
    public RunLogCollectorFactory buildTaskLogCollectorFactory() {
        return new RunLogCollectorFactoryDocker(dockerClientFinder, containerRunMapper);
    }


}
