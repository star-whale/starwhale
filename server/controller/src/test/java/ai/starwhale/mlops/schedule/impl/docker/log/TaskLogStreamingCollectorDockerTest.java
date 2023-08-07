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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.docker.ContainerTaskMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@Slf4j
public class TaskLogStreamingCollectorDockerTest {

    DockerClient dockerClient;

    DockerClientFinder dockerClientFinder;

    TaskLogStreamingCollectorDocker logStreamingCollector;

    static final String IMAGE_HELLO_WORLD = "hello-world:linux";

    static String containerName = UUID.randomUUID().toString();

    @BeforeEach
    public void setup() {
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
        dockerClientFinder = mock(DockerClientFinder.class);
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(this.dockerClient);
    }

    static final String OUT_PUT_HELLO_WORLD = "STDOUT: \n"
            + "STDOUT: Hello from Docker!\n"
            + "STDOUT: This message shows that your installation appears to be working correctly.\n"
            + "STDOUT: \n"
            + "STDOUT: To generate this message, Docker took the following steps:\n"
            + "STDOUT: 1. The Docker client contacted the Docker daemon.\n"
            + "STDOUT: 2. The Docker daemon pulled the \"hello-world\" image from the Docker Hub.\n"
            + "STDOUT: (amd64)\n"
            + "STDOUT: 3. The Docker daemon created a new container from that image which runs the\n"
            + "STDOUT: executable that produces the output you are currently reading.\n"
            + "STDOUT: 4. The Docker daemon streamed that output to the Docker client, which sent it\n"
            + "STDOUT: to your terminal.\n"
            + "STDOUT: \n"
            + "STDOUT: To try something more ambitious, you can run an Ubuntu container with:\n"
            + "STDOUT: $ docker run -it ubuntu bash\n"
            + "STDOUT: \n"
            + "STDOUT: Share images, automate workflows, and more with a free Docker ID:\n"
            + "STDOUT: https://hub.docker.com/\n"
            + "STDOUT: \n"
            + "STDOUT: For more examples and ideas, visit:\n"
            + "STDOUT: https://docs.docker.com/get-started/\n"
            + "STDOUT: \n";

    @Test
    public void testReadLine() throws InterruptedException, IOException {
        Object lock = new Object();
        List<String> rl = new ArrayList<>();
        dockerClient.pullImageCmd(IMAGE_HELLO_WORLD).exec(new ResultCallback<PullResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {
            }

            @Override
            public void onNext(PullResponseItem object) {

            }

            @Override
            public void onError(Throwable throwable) {

                synchronized (lock) {
                    log.warn("pulling image {} failed", IMAGE_HELLO_WORLD);
                    lock.notifyAll();
                }

            }

            @Override
            public void onComplete() {
                CreateContainerResponse exec = dockerClient.createContainerCmd(
                                IMAGE_HELLO_WORLD
                        )
                        .withName(containerName)
                        .exec();
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
        doCollectLog(containerName);
    }

//    @Test
//    public void testOnly() throws IOException {
//        doCollectLog("b6bc58b5-7e93-4708-b101-86e97197f019");
//    }

    private void doCollectLog(String containerName) throws IOException {
        ContainerTaskMapper containerTaskMapper = mock(ContainerTaskMapper.class);
        Task task = Task.builder().id(1L).step(new Step()).build();

        when(containerTaskMapper.containerNameOfTask(task)).thenReturn(containerName);
        when(containerTaskMapper.taskIfOfContainer(containerName)).thenReturn(1L);
        logStreamingCollector = new TaskLogStreamingCollectorDocker(task, dockerClientFinder, containerTaskMapper);

        String log = "";
        StringBuilder wholeLog = new StringBuilder();
        while (null != log) {
            log = logStreamingCollector.readLine(null);
            if (null != log) {
                wholeLog.append(log);
                wholeLog.append("\n");
            }

        }
        logStreamingCollector.cancel();

        Assertions.assertTrue(wholeLog.toString().contains(OUT_PUT_HELLO_WORLD));
        dockerClient.removeContainerCmd(containerName).withForce(true).withRemoveVolumes(true).exec();
    }

}
