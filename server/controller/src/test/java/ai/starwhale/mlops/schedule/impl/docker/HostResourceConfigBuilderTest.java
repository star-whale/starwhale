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

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HostResourceConfigBuilderTest {

    static final String IMAGE_BUSY_BOX = "busybox:latest";

    @Test
    public void testRun() {
        LocalDockerTool localDockerTool = new LocalDockerTool();
        String containerName = "sw-ut-HostResourceConfigBuilderTest" + System.currentTimeMillis();
        HostResourceConfigBuilder builder = new HostResourceConfigBuilder();
        HostConfig hostConfig = builder.build(
                List.of(RuntimeResource.builder().type("memory").limit(1024 * 1024 * 10f).build(),
                        RuntimeResource.builder().type("cpu").limit(1f).build()));
        try (var tc = localDockerTool.startContainerBlocking(IMAGE_BUSY_BOX, containerName, Map.of(),
                new String[]{"tail", "-f", "/dev/null"}, hostConfig)) {
            InspectContainerResponse response = tc.dockerClient.inspectContainerCmd(tc.name).exec();
            Assertions.assertEquals(1L, response.getHostConfig().getCpuCount());
            Assertions.assertEquals(1024 * 1024 * 10L, response.getHostConfig().getMemory());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
