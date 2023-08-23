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
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.configuration.RunTimeProperties.RunConfig;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.schedule.impl.container.impl.DatasetBuildContainerSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatasetBuildContainerSpecificationBuilderTest {

    SystemSettingService systemSettingService;
    TaskTokenValidator taskTokenValidator;

    String instanceUri = "10.2.2.3:8080";
    RunTimeProperties runTimeProperties;

    Task task;

    static final String NAME = "dataset_build";

    DatasetBuildContainerSpecification dsbb;

    @BeforeEach
    public void setup() {
        mockTask();
        mockSysSetting();
        mockTtv();
        dsbb = new DatasetBuildContainerSpecification(systemSettingService, instanceUri, taskTokenValidator, task);
    }

    private void mockTtv() {
        taskTokenValidator = mock(TaskTokenValidator.class);
        when(taskTokenValidator.getTaskToken(any(), any())).thenReturn("aabbcc");
    }

    private void mockSysSetting() {
        systemSettingService = mock(SystemSettingService.class);
        runTimeProperties = mock(RunTimeProperties.class);
        when(systemSettingService.getRunTimeProperties()).thenReturn(runTimeProperties);
        when(systemSettingService.getDockerSetting()).thenReturn(new DockerSetting());
    }

    private void mockTask() {
        task = mock(Task.class);
        when(task.getTaskRequest()).thenReturn(TaskRequest.builder().env(List.of(new Env("k", "v"))).build());
        when(task.getStep()).thenReturn(Step.builder()
                .job(Job.builder().project(Project.builder().name("p").build()).virtualJobName(NAME).build()).build());
    }

    @Test
    public void testNoSystemSetting() {
        when(runTimeProperties.getDatasetBuild()).thenReturn(null);
        when(runTimeProperties.getPypi()).thenReturn(null);
        Map<String, String> envs = dsbb.getContainerEnvs();
        Assertions.assertEquals("v", envs.get("k"));
        Assertions.assertEquals("", envs.get("SW_VERSION"));
        Assertions.assertEquals("", envs.get("SW_RUNTIME_PYTHON_VERSION"));
        Assertions.assertEquals(null, envs.get("SW_PYPI_INDEX_URL"));
        Assertions.assertEquals(null, envs.get("SW_PYPI_EXTRA_INDEX_URL"));
        Assertions.assertEquals(null, envs.get("SW_PYPI_TRUSTED_HOST"));
        Assertions.assertEquals(null, envs.get("SW_PYPI_TIMEOUT"));
        Assertions.assertEquals(null, envs.get("SW_PYPI_RETRIES"));
        Assertions.assertEquals(instanceUri, envs.get("SW_INSTANCE_URI"));
        Assertions.assertEquals("p", envs.get("SW_PROJECT"));
        Assertions.assertEquals("aabbcc", envs.get("SW_TOKEN"));
        Assertions.assertEquals("docker-registry.starwhale.cn/star-whale/starwhale:latest", dsbb.getImage());
        Assertions.assertIterableEquals(List.of("dataset_build"), Arrays.asList(dsbb.getCmd().getCmd()));
        Assertions.assertNull(dsbb.getCmd().getEntrypoint());
    }

    @Test
    public void testWithDsBuildSystemSetting() {
        when(runTimeProperties.getDatasetBuild()).thenReturn(
                new RunConfig("rp", "abc.com/testImagxe", "a0.5.1", "a2.11"));
        when(runTimeProperties.getPypi()).thenReturn(new Pypi("idu", "exdu", "th", 10, 90));
        Map<String, String> envs = dsbb.getContainerEnvs();
        Assertions.assertEquals("v", envs.get("k"));
        Assertions.assertEquals("a0.5.1", envs.get("SW_VERSION"));
        Assertions.assertEquals("a2.11", envs.get("SW_RUNTIME_PYTHON_VERSION"));
        Assertions.assertEquals("idu", envs.get("SW_PYPI_INDEX_URL"));
        Assertions.assertEquals("exdu", envs.get("SW_PYPI_EXTRA_INDEX_URL"));
        Assertions.assertEquals("th", envs.get("SW_PYPI_TRUSTED_HOST"));
        Assertions.assertEquals("90", envs.get("SW_PYPI_TIMEOUT"));
        Assertions.assertEquals("10", envs.get("SW_PYPI_RETRIES"));
        Assertions.assertEquals(instanceUri, envs.get("SW_INSTANCE_URI"));
        Assertions.assertEquals("p", envs.get("SW_PROJECT"));
        Assertions.assertEquals("aabbcc", envs.get("SW_TOKEN"));
        Assertions.assertEquals("abc.com/testImagxe", dsbb.getImage());
        Assertions.assertIterableEquals(List.of("dataset_build"), Arrays.asList(dsbb.getCmd().getCmd()));
        Assertions.assertNull(dsbb.getCmd().getEntrypoint());
    }

}
