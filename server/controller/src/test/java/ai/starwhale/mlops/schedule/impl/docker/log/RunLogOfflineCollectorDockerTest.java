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

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.schedule.impl.docker.ContainerRunMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.impl.docker.LocalDockerTool;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import io.vavr.Tuple2;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@Slf4j
public class RunLogOfflineCollectorDockerTest {

    static final String IMAGE_HELLO_WORLD = "hello-world:linux";
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
    static String containerName = UUID.randomUUID().toString();
    DockerClient dockerClient;
    LocalDockerTool localDockerTool = new LocalDockerTool();
    DockerClientFinder dockerClientFinder;
    RunLogOfflineCollectorDocker logOfflineCollectorDocker;

    @BeforeEach
    public void setup() {
        this.dockerClient = localDockerTool.getDockerClient();
        dockerClientFinder = mock(DockerClientFinder.class);
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(this.dockerClient);
        ContainerRunMapper containerTaskMapper = mock(ContainerRunMapper.class);
        Run run = Run.builder().runSpec(RunSpec.builder().build()).id(1L).build();

        Container container = mock(Container.class);
        when(container.getId()).thenReturn(containerName);
        when(container.getNames()).thenReturn(new String[]{containerName});
        when(containerTaskMapper.containerOfRun(run)).thenReturn(container);
        when(containerTaskMapper.runIdOfContainer(container)).thenReturn(1L);
        logOfflineCollectorDocker = new RunLogOfflineCollectorDocker(run, dockerClientFinder, containerTaskMapper);
    }

    @Test
    public void testOfflineLog() throws InterruptedException {
        try (var td = localDockerTool.startContainerBlocking(IMAGE_HELLO_WORLD,
                containerName, Map.of(), null, null)) {
            Tuple2<String, String> stringTuple2 = logOfflineCollectorDocker.collect();
            Assertions.assertEquals(containerName, stringTuple2._1());
            Assertions.assertEquals(OUT_PUT_HELLO_WORLD, stringTuple2._2());
        }
    }


}
