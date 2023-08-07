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

import static ai.starwhale.mlops.schedule.impl.docker.SwTaskSchedulerDocker.CONTAINER_LABELS;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DockerClientTest {

    DockerClient dockerClient;

    @BeforeEach
    public void setup(){
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


    @Test
    public void test(){
        CreateContainerResponse exec = dockerClient.createContainerCmd(
                        "docker-registry.starwhale.cn/star-whale/starwhale:0.5.6"
                )
                .withCmd("run")
//                .withEnv(buildEnvs(task))
//                .withHostConfig(buildHostConfig(task))
                .withLabels(CONTAINER_LABELS)
                .exec();
        System.out.println(exec.getId());
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(exec.getId());
        startContainerCmd.exec();
    }
    @Test
    public void tr(){
        List<Container> containers = dockerClient.listContainersCmd().withLabelFilter(CONTAINER_LABELS).withShowAll(true).exec();
        containers.forEach(c->{
           System.out.println(c.getNames()[0]);
        });
    }

    @Test void info() throws InterruptedException {
        dockerClient.logContainerCmd("unruffled_swanson").withFollowStream(false).withStdErr(true).withStdOut(true).exec(new ResultCallback<Frame>() {
            @Override
            public void onStart(Closeable closeable) {
                System.out.println("start");

            }

            @Override
            public void onNext(Frame object) {

                System.out.println(object.toString());

            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println(throwable);


            }

            @Override
            public void onComplete() {
                System.out.println("complete");
            }

            @Override
            public void close() throws IOException {
                System.out.println("close");

            }
        });
        Thread.sleep(1000);
    }


}
