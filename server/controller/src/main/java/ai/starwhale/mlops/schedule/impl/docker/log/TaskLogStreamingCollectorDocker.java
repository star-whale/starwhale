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
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TaskLogStreamingCollectorDocker implements TaskLogStreamingCollector {

    final DockerClient dockerClient;

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    final BlockingQueue<String> logLines;

    Closeable closeable;

    Boolean closed = Boolean.FALSE;

    public TaskLogStreamingCollectorDocker(Task task, DockerClientFinder dockerClientFinder,
            ContainerTaskMapper containerTaskMapper) {
        this.dockerClientFinder = dockerClientFinder;
        this.dockerClient = this.dockerClientFinder.findProperDockerClient(task.getStep().getResourcePool());
        this.containerTaskMapper = containerTaskMapper;
        this.logLines = new LinkedBlockingQueue<>();
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(
                        this.containerTaskMapper.containerNameOfTask(task))
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true);
        var that = this;
        logContainerCmd.exec(new ResultCallback<Frame>() {
            @Override
            public void onStart(Closeable closeable) {
                that.closeable = closeable;
            }

            @Override
            public void onNext(Frame object) {
                try {
                    logLines.put(object.toString());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                signalClose();
            }

            @Override
            public void onComplete() {
                signalClose();
            }

            @Override
            public void close() throws IOException {
                signalClose();
            }

            private void signalClose() {
                closed = Boolean.TRUE;
                try {
                    logLines.put("");
                } catch (InterruptedException e) {
                    log.error("putting to logLines failed, readLine thread may starving");
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public String readLine(Long waitTimeSeconds) throws IOException {
        if (this.closed) {
            return null;
        }
        try {
            if (null == waitTimeSeconds) {
                return logLines.take();
            } else {
                return this.logLines.poll(waitTimeSeconds, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            log.error("taking log line Interrupted", e);
            return null;
        }
    }

    @Override
    public void cancel() {
        try {
            if (null != this.closeable) {
                this.closeable.close();
            }
            this.logLines.clear();
            this.closed = Boolean.TRUE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
