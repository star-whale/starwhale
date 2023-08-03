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

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
import java.util.Collection;
import java.util.concurrent.Future;

public class SwTaskSchedulerDocker implements SwTaskScheduler {

    final DockerClient dockerClient;

    public SwTaskSchedulerDocker(DockerClient dockerClient){
         this.dockerClient = dockerClient;
    }

    @Override
    public void schedule(Collection<Task> tasks, TaskReportReceiver taskReportReceiver) {
    }

    @Override
    public void stop(Collection<Task> tasks) {

    }

    @Override
    public Future<String[]> exec(Task task, String... command) {
        return null;
    }
}
