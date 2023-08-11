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

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContainerTaskMapperTest {

    DockerClientFinder dockerClientFinder;
    DockerClient dockerClient;

    LocalDockerTool localDockerTool = new LocalDockerTool();

    @BeforeEach
    public void setup() {
        dockerClientFinder = mock(DockerClientFinder.class);
        this.dockerClient = localDockerTool.getDockerClient();
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(dockerClient);
    }

    static final String IMAGE_HELLO_WORLD = "hello-world:linux";

    @Test
    public void testContainerExistedOne() throws InterruptedException {
        ContainerTaskMapper cm = new ContainerTaskMapper(dockerClientFinder);
        String containerName = "sw-ut-container";
        try (var tc = localDockerTool.startContainerBlocking(IMAGE_HELLO_WORLD, containerName,
                Map.of("starwhale-task-id", "1"), null, null)) {
            Task task = Task.builder().id(1L).step(new Step()).build();
            Container container = cm.containerOfTask(task);
            Assertions.assertEquals("/" + containerName, container.getNames()[0]);
            Assertions.assertEquals(1L, cm.taskIfOfContainer(container));
        }


    }

    @Test
    public void testContainerNotExisted() throws InterruptedException {
        ContainerTaskMapper cm = new ContainerTaskMapper(dockerClientFinder);
        Task task = Task.builder().id(123344L).step(new Step()).build();
        Container container = cm.containerOfTask(task);
        Assertions.assertNull(container);
    }
}
