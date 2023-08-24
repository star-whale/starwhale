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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.spec.ContainerSpec;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.impl.RuntimeDockerizedContainerSpecification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuntimeDockerizedContainerSpecificationTest {

    SystemSettingService systemSettingService;
    TaskTokenValidator taskTokenValidator;

    String instanceUri = "10.2.2.3:8080";
    RuntimeVersionMapper runtimeVersionMapper;
    RunTimeProperties runTimeProperties;

    Task task;

    static final String NAME = "runtime_dockerizing";

    RuntimeDockerizedContainerSpecification rdcs;


    @BeforeEach
    public void setup() {
        mockTask();
        mockSysSetting();
        mockTtv();
        rdcs = new RuntimeDockerizedContainerSpecification(
                task,
                instanceUri,
                systemSettingService,
                taskTokenValidator,
                runtimeVersionMapper
        );
    }

    private void mockTtv() {
        taskTokenValidator = mock(TaskTokenValidator.class);
        when(taskTokenValidator.getTaskToken(any(), any())).thenReturn("aabbcc");
    }

    private void mockSysSetting() {
        systemSettingService = mock(SystemSettingService.class);
        runTimeProperties = mock(RunTimeProperties.class);
        when(systemSettingService.getRunTimeProperties()).thenReturn(runTimeProperties);
        when(systemSettingService.getDockerSetting()).thenReturn(new DockerSetting("rpl", "rph", "un", "pwd", true));
    }

    private void mockTask() {
        task = mock(Task.class);
        when(task.getTaskRequest()).thenReturn(TaskRequest.builder().env(List.of(new Env("k", "v"))).build());
        when(task.getStep()).thenReturn(Step.builder()
                                                .spec(StepSpec.builder()
                                                              .containerSpec(ContainerSpec.builder()
                                                                                     .image("img")
                                                                                     .cmds(new String[]{"c1"})
                                                                                     .entrypoint(new String[]{"e1"})
                                                                                     .build())
                                                              .build())
                                                .job(Job.builder()
                                                             .project(Project.builder().name("p").build())
                                                             .virtualJobName(NAME)
                                                             .build()).build());
    }

    @Test
    public void testGetContainerEnvs() {
        Map<String, String> containerEnvs = rdcs.getContainerEnvs();
        Assertions.assertEquals("10.2.2.3:8080", containerEnvs.get("SW_INSTANCE_URI"));
        Assertions.assertEquals("p", containerEnvs.get("SW_PROJECT"));
        Assertions.assertEquals("aabbcc", containerEnvs.get("SW_TOKEN"));
        Assertions.assertEquals("v", containerEnvs.get("k"));
        Assertions.assertEquals("rph/cache", containerEnvs.get("SW_CACHE_REPO"));
        Assertions.assertEquals(
                "{\"auths\":{\"rph\":{\"password\":\"pwd\",\"username\":\"un\"}}}",
                containerEnvs.get("SW_DOCKER_REGISTRY_KEYS")
        );
        Assertions.assertEquals("true", containerEnvs.get("SW_DOCKER_REGISTRY_INSECURE"));
    }

    @Test
    public void testGetImage() {
        Assertions.assertEquals("img", rdcs.getImage());
    }

    @Test
    public void testGetCmd() {
        ContainerCommand cmd = rdcs.getCmd();
        Assertions.assertIterableEquals(List.of("c1"), List.of(cmd.getCmd()));
        Assertions.assertIterableEquals(List.of("e1"), List.of(cmd.getEntrypoint()));
    }


}
