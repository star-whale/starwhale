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

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.schedule.impl.docker.ContainerTaskMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.log.TaskLogCollectorFactory;
import ai.starwhale.mlops.schedule.log.TaskLogOfflineCollector;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;

public class TaskLogCollectorFactoryDocker implements TaskLogCollectorFactory {

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    public TaskLogCollectorFactoryDocker(DockerClientFinder dockerClientFinder, ContainerTaskMapper containerTaskMapper) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
    }

    @Override
    public TaskLogOfflineCollector offlineCollector(Task task) throws StarwhaleException {
        return new TaskLogOfflineCollectorDocker(task,dockerClientFinder, containerTaskMapper);
    }

    @Override
    public TaskLogStreamingCollector streamingCollector(Task task) throws StarwhaleException {
        return new TaskLogStreamingCollectorDocker(task,dockerClientFinder, containerTaskMapper);
    }
}
