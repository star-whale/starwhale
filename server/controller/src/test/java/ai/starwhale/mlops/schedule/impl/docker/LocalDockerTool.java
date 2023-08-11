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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;


@Slf4j
public class LocalDockerTool {

    final DockerClient dockerClient;

    public LocalDockerTool() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        this.dockerClient = DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    public DockerClient getDockerClient() {
        return this.dockerClient;
    }

    public TempDockerContainer startContainerBlocking(String image, String containerName, Map<String, String> labels,
            String[] cmds, HostConfig hostConfig)
            throws InterruptedException {
        Object lock = new Object();
        List<String> rl = new ArrayList<>();
        dockerClient.pullImageCmd(image).exec(new ResultCallback<PullResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {
            }

            @Override
            public void onNext(PullResponseItem object) {

            }

            @Override
            public void onError(Throwable throwable) {

                synchronized (lock) {
                    log.error("pulling image {} failed", image, throwable);
                    lock.notifyAll();
                }

            }

            @Override
            public void onComplete() {
                CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(
                                image
                        )
                        .withName(containerName);
                if (!CollectionUtils.isEmpty(labels)) {
                    createContainerCmd.withLabels(labels);
                }
                if (null != cmds && cmds.length > 0) {
                    createContainerCmd.withCmd(cmds);
                }
                if (null != hostConfig) {
                    createContainerCmd.withHostConfig(hostConfig);
                }
                CreateContainerResponse exec = createContainerCmd.exec();
                StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(exec.getId());
                startContainerCmd.exec();
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

        synchronized (lock) {
            lock.wait();
        }
        return new TempDockerContainer(this.dockerClient, containerName);
    }

    public static class TempDockerContainer implements AutoCloseable {

        final DockerClient dockerClient;

        final String name;

        public TempDockerContainer(DockerClient dockerClient, String name) {
            this.dockerClient = dockerClient;
            this.name = name;
        }

        @Override
        public void close() {
            dockerClient.removeContainerCmd(name).withForce(true).withRemoveVolumes(true).exec();
        }
    }

}
