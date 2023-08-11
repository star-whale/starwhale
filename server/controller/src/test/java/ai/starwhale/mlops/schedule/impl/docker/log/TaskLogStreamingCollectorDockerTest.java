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
import ai.starwhale.mlops.schedule.impl.docker.LocalDockerTool;
import ai.starwhale.mlops.schedule.impl.docker.LocalDockerTool.TempDockerContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@Slf4j
public class TaskLogStreamingCollectorDockerTest {

    static final String IMAGE_HELLO_WORLD = "hello-world:linux";
    static final String OUT_PUT_HELLO_WORLD = "Hello from Docker!\n"
            + "This message shows that your installation appears to be working correctly.\n"
            + "\n"
            + "To generate this message, Docker took the following steps:\n"
            + " 1. The Docker client contacted the Docker daemon.\n"
            + " 2. The Docker daemon pulled the \"hello-world\" image from the Docker Hub.\n"
            + "    (amd64)\n"
            + " 3. The Docker daemon created a new container from that image which runs the\n"
            + "    executable that produces the output you are currently reading.\n"
            + " 4. The Docker daemon streamed that output to the Docker client, which sent it\n"
            + "    to your terminal.\n"
            + "\n"
            + "To try something more ambitious, you can run an Ubuntu container with:\n"
            + " $ docker run -it ubuntu bash\n"
            + "\n"
            + "Share images, automate workflows, and more with a free Docker ID:\n"
            + " https://hub.docker.com/\n"
            + "\n"
            + "For more examples and ideas, visit:\n"
            + " https://docs.docker.com/get-started/\n";
    static String containerName = UUID.randomUUID().toString();
    DockerClient dockerClient;
    DockerClientFinder dockerClientFinder;

    LocalDockerTool localDockerTool = new LocalDockerTool();
    TaskLogStreamingCollectorDocker logStreamingCollector;

    @BeforeEach
    public void setup() {
        this.dockerClient = localDockerTool.getDockerClient();
        dockerClientFinder = mock(DockerClientFinder.class);
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(this.dockerClient);
    }

    @Test
    public void testReadLine() throws InterruptedException, IOException {
        try (TempDockerContainer tempDockerContainer = localDockerTool.startContainerBlocking(IMAGE_HELLO_WORLD,
                containerName, Map.of(), null, null)) {
            doCollectLog(containerName);
        }

    }

    private void doCollectLog(String containerName) throws IOException {
        ContainerTaskMapper containerTaskMapper = mock(ContainerTaskMapper.class);
        Task task = Task.builder().id(1L).step(new Step()).build();
        Container container = mock(Container.class);
        when(container.getId()).thenReturn(containerName);

        when(containerTaskMapper.containerOfTask(task)).thenReturn(container);
        when(containerTaskMapper.taskIfOfContainer(container)).thenReturn(1L);
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
        String wholeLogStr = wholeLog.toString();
        Arrays.stream(OUT_PUT_HELLO_WORLD.split("\n")).forEach(l -> {
            Assertions.assertTrue(wholeLogStr.contains(l.strip()));
        });

    }

}
