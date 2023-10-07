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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContainerRunMapperTest {

    DockerClientFinder dockerClientFinder;
    DockerClient dockerClient;

    LocalDockerTool localDockerTool = new LocalDockerTool();

    @BeforeEach
    public void setup() {
        dockerClientFinder = mock(DockerClientFinder.class);
        this.dockerClient = localDockerTool.getDockerClient();
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(dockerClient);
    }

    @Test
    public void testContainerExistedOne() throws InterruptedException {
        ContainerRunMapper cm = new ContainerRunMapper(dockerClientFinder);
        String containerName = "sw-ut-container";
        try (var tc = localDockerTool.startContainerBlocking("busybox:latest", containerName,
                Map.of("starwhale-run-id", "1"), new String[]{"sleep", "100000000"}, null)) {
            Run run = Run.builder().id(1L).runSpec(RunSpec.builder().build()).build();
            Container container = cm.containerOfRun(run);
            Assertions.assertEquals("/" + containerName, container.getNames()[0]);
            Assertions.assertEquals(1L, cm.runIdOfContainer(container));
        }


    }

    @Test
    public void testContainerNotExisted() throws InterruptedException {
        ContainerRunMapper cm = new ContainerRunMapper(dockerClientFinder);
        Run run = Run.builder().id(123344L).runSpec(RunSpec.builder().build()).build();
        Container container = cm.containerOfRun(run);
        Assertions.assertNull(container);
    }
}
