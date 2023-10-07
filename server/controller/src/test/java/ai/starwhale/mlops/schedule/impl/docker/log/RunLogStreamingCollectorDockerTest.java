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
import ai.starwhale.mlops.schedule.impl.docker.LocalDockerTool.TempDockerContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@Slf4j
public class RunLogStreamingCollectorDockerTest {

    static final String IMAGE_HELLO_WORLD = "busybox:latest";
    static final String OUT_PUT_HELLO_WORLD = "hello\nworld";
    static String containerName = UUID.randomUUID().toString();
    DockerClient dockerClient;
    DockerClientFinder dockerClientFinder;

    LocalDockerTool localDockerTool = new LocalDockerTool();
    RunLogStreamingCollectorDocker logStreamingCollector;

    @BeforeEach
    public void setup() {
        this.dockerClient = localDockerTool.getDockerClient();
        dockerClientFinder = mock(DockerClientFinder.class);
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(this.dockerClient);
    }

    @Test
    public void testReadLine() throws InterruptedException, IOException {
        try (TempDockerContainer tempDockerContainer = localDockerTool.startContainerBlocking(IMAGE_HELLO_WORLD,
                containerName, Map.of(), new String[]{"echo", OUT_PUT_HELLO_WORLD}, null)) {
            doCollectLog(containerName);
        }
    }

    private void doCollectLog(String containerName) throws IOException, InterruptedException {
        ContainerRunMapper containerTaskMapper = mock(ContainerRunMapper.class);
        Run run = Run.builder().id(1L).runSpec(RunSpec.builder().build()).build();
        Container container = mock(Container.class);
        when(container.getId()).thenReturn(containerName);

        when(containerTaskMapper.containerOfRun(run)).thenReturn(container);
        when(containerTaskMapper.runIdOfContainer(container)).thenReturn(1L);
        logStreamingCollector = new RunLogStreamingCollectorDocker(run, dockerClientFinder, containerTaskMapper);

        // sleep for a while to wait for the log collector done,
        // and check if there is no bug in the log collector `close` signal
        Thread.sleep(1000);

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
        Assertions.assertEquals(OUT_PUT_HELLO_WORLD, wholeLogStr.strip());
    }
}
