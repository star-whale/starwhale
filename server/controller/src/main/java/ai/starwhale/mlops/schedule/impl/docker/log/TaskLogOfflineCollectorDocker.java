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
import ai.starwhale.mlops.schedule.impl.docker.ContainerTaskMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.log.TaskLogOfflineCollector;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import io.vavr.Tuple2;
import java.io.Closeable;
import java.io.IOException;

public class TaskLogOfflineCollectorDocker implements TaskLogOfflineCollector {

    final Task task;

    final DockerClient dockerClient;

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    final Object lock = new Object();

    StringBuffer logBuffer = new StringBuffer();


    public TaskLogOfflineCollectorDocker(Task task, DockerClientFinder dockerClientFinder,
            ContainerTaskMapper containerTaskMapper) {
        this.task = task;
        this.dockerClientFinder = dockerClientFinder;
        this.dockerClient = this.dockerClientFinder.findProperDockerClient(task.getStep().getResourcePool());
        this.containerTaskMapper = containerTaskMapper;
    }

    @Override
    public Tuple2<String, String> collect() {
        logBuffer = new StringBuffer();
        Container container = this.containerTaskMapper.containerOfTask(task);
        if (null == container) {
            return null;
        }
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(
                        container.getId())
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(false);
        logContainerCmd.exec(new ResultCallback<Frame>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(Frame object) {
                logBuffer.append(object.toString());
                logBuffer.append("\n");
            }

            @Override
            public void onError(Throwable throwable) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void onComplete() {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void close() throws IOException {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        });
        synchronized (this.lock) {
            try {
                this.lock.wait();
                return new Tuple2<>(container.getNames()[0], this.logBuffer.toString());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
