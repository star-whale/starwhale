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

package ai.starwhale.mlops.schedule.impl.contianer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.spec.ContainerSpec;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.impl.CustomContainerSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomContainerSpecificationTest {

    Task task;

    CustomContainerSpecification customTaskContainerEntrypointBuilder;

    @BeforeEach
    public void setup() {
        mockTask();
        customTaskContainerEntrypointBuilder = new CustomContainerSpecification(task);
    }

    @Test
    public void testAllFilled() {
        ContainerCommand cmd = customTaskContainerEntrypointBuilder.getCmd();
        Assertions.assertIterableEquals(List.of("-c", "echo 'hi'"), Arrays.asList(cmd.getCmd()));
        Assertions.assertIterableEquals(List.of("bash"), Arrays.asList(cmd.getEntrypoint()));
        Map<String, String> envs = customTaskContainerEntrypointBuilder.getContainerEnvs();
        Assertions.assertEquals("v", envs.get("k"));
    }

    @Test
    public void testEpMiss() {
        when(task.getStep()).thenReturn(Step.builder()
                .spec(StepSpec.builder()
                        .containerSpec(
                                ContainerSpec.builder()
                                        .cmds(new String[]{"-c", "echo 'hi'"})
                                        .image("abc.com/image")
                                        .build())
                        .env(List.of(new Env("k", "v")))
                        .build()).build());
        customTaskContainerEntrypointBuilder = new CustomContainerSpecification(task);
        ContainerCommand cmd = customTaskContainerEntrypointBuilder.getCmd();
        Assertions.assertIterableEquals(List.of("-c", "echo 'hi'"), Arrays.asList(cmd.getCmd()));
        Assertions.assertNull(cmd.getEntrypoint());
        Map<String, String> envs = customTaskContainerEntrypointBuilder.getContainerEnvs();
        Assertions.assertEquals("v", envs.get("k"));
    }

    @Test
    public void testFieldMiss() {
        when(task.getStep()).thenReturn(Step.builder()
                .spec(StepSpec.builder()
                        .containerSpec(
                                ContainerSpec.builder()
                                        .image("abc.com/image")
                                        .build())
                        .build()).build());
        customTaskContainerEntrypointBuilder = new CustomContainerSpecification(task);
        ContainerCommand cmd = customTaskContainerEntrypointBuilder.getCmd();
        Assertions.assertNull(cmd.getCmd());
        Assertions.assertNull(cmd.getEntrypoint());
        Map<String, String> envs = customTaskContainerEntrypointBuilder.getContainerEnvs();
        Assertions.assertNull(envs.get("k"));
    }

    private void mockTask() {
        task = mock(Task.class);
        when(task.getStep()).thenReturn(Step.builder()
                .spec(StepSpec.builder()
                        .containerSpec(
                                ContainerSpec.builder().entrypoint(new String[]{"bash"})
                                        .cmds(new String[]{"-c", "echo 'hi'"})
                                        .image("abc.com/image")
                                        .build())
                        .env(List.of(new Env("k", "v")))
                        .build()).build());
    }

}
