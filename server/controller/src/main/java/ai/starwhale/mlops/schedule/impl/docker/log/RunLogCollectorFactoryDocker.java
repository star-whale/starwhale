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

package ai.starwhale.mlops.schedule.impl.docker.log;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.schedule.impl.docker.ContainerRunMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.log.RunLogCollectorFactory;
import ai.starwhale.mlops.schedule.log.RunLogOfflineCollector;
import ai.starwhale.mlops.schedule.log.RunLogStreamingCollector;

public class RunLogCollectorFactoryDocker implements RunLogCollectorFactory {

    final DockerClientFinder dockerClientFinder;

    final ContainerRunMapper containerRunMapper;

    public RunLogCollectorFactoryDocker(
            DockerClientFinder dockerClientFinder,
            ContainerRunMapper containerRunMapper
    ) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerRunMapper = containerRunMapper;
    }

    @Override
    public RunLogOfflineCollector offlineCollector(Run run) throws StarwhaleException {
        return new RunLogOfflineCollectorDocker(run, dockerClientFinder, containerRunMapper);
    }

    @Override
    public RunLogStreamingCollector streamingCollector(Run run) throws StarwhaleException {
        return new RunLogStreamingCollectorDocker(run, dockerClientFinder, containerRunMapper);
    }
}
