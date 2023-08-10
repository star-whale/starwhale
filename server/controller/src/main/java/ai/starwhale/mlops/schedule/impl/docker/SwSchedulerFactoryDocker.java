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

import ai.starwhale.mlops.schedule.SwSchedulerAbstractFactory;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.TaskCommandGetter;
import ai.starwhale.mlops.schedule.TaskRunningEnvBuilder;
import ai.starwhale.mlops.schedule.impl.docker.log.TaskLogCollectorFactoryDocker;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerTaskReporter;
import ai.starwhale.mlops.schedule.log.TaskLogCollectorFactory;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(value = "sw.scheduler", havingValue = "docker")
public class SwSchedulerFactoryDocker implements SwSchedulerAbstractFactory {

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    final DockerTaskReporter dockerTaskReporter;

    final TaskRunningEnvBuilder taskRunningEnvBuilder;

    final String network;
    final String nodeIp;
    final TaskCommandGetter taskCommandGetter;

    public SwSchedulerFactoryDocker(DockerClientFinder dockerClientFinder, ContainerTaskMapper containerTaskMapper,
            DockerTaskReporter dockerTaskReporter,
            TaskRunningEnvBuilder taskRunningEnvBuilder, @Value("${sw.infra.docker.network}") String network,
            @Value("${sw.infra.docker.node-ip}") String nodeIp,
            TaskCommandGetter taskCommandGetter) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
        this.dockerTaskReporter = dockerTaskReporter;
        this.taskRunningEnvBuilder = taskRunningEnvBuilder;
        this.network = network;
        this.nodeIp = nodeIp;
        this.taskCommandGetter = taskCommandGetter;
    }

    @Bean
    @Override
    public SwTaskScheduler buildSwTaskScheduler() {
        return new SwTaskSchedulerDocker(dockerClientFinder, containerTaskMapper, dockerTaskReporter,
                Executors.newCachedThreadPool(), taskRunningEnvBuilder, network,
                nodeIp, taskCommandGetter, new HostResourceConfigBuilder());
    }

    @Bean
    @Override
    public TaskLogCollectorFactory buildTaskLogCollectorFactory() {
        return new TaskLogCollectorFactoryDocker(dockerClientFinder, containerTaskMapper);
    }


}
