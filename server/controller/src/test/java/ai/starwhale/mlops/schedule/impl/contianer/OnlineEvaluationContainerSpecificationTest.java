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

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeService.RuntimeManifest;
import ai.starwhale.mlops.domain.runtime.RuntimeService.RuntimeManifest.Environment;
import ai.starwhale.mlops.domain.runtime.RuntimeService.RuntimeManifest.Lock;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.schedule.impl.container.impl.OnlineEvaluationContainerSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OnlineEvaluationContainerSpecificationTest {

    Task task;

    TaskTokenValidator taskTokenValidator;

    String instanceUri = "10.2.2.3:8080";

    RunTimeProperties runTimeProperties;

    OnlineEvaluationContainerSpecification oecs;

    static final String NAME = "online_eval";

    @BeforeEach
    public void setup() {
        mockTask();
        mockTtv();
        mockSysSetting();
        oecs = new OnlineEvaluationContainerSpecification(
                task,
                taskTokenValidator,
                instanceUri,
                runTimeProperties
        );

    }

    private void mockTtv() {
        taskTokenValidator = mock(TaskTokenValidator.class);
        when(taskTokenValidator.getTaskToken(any(), any())).thenReturn("aabbcc");
    }

    private void mockSysSetting() {
        runTimeProperties = mock(RunTimeProperties.class);
        when(runTimeProperties.getPypi()).thenReturn(new Pypi());
    }

    private void mockTask() {
        task = mock(Task.class);
        when(task.getTaskRequest()).thenReturn(TaskRequest.builder().env(List.of(new Env("k", "v"))).build());
        RuntimeManifest rtManifest = new RuntimeManifest();
        rtManifest.setBaseImage("img2");
        Environment environment = new Environment();
        environment.setPython("3.9");
        Lock lock = new Lock();
        lock.setSwVersion("0.3.3");
        environment.setLock(lock);
        rtManifest.setEnvironment(environment);
        when(task.getStep()).thenReturn(Step.builder()
                                                .job(Job.builder()
                                                             .project(Project.builder().name("p").build())
                                                             .virtualJobName(NAME)
                                                             .owner(User.builder().id(1L).build())
                                                             .model(Model.builder()
                                                                            .name("md_n")
                                                                            .version("md_v")
                                                                            .build())
                                                             .jobRuntime(JobRuntime.builder()
                                                                                 .name("rt_n")
                                                                                 .version("rt_v")
                                                                                 .image("img")
                                                                                 .manifest(rtManifest)
                                                                                 .build())
                                                             .build()).build());
    }

    @Test
    public void testGetCmd() {
        Assertions.assertIterableEquals(List.of("serve"), Arrays.asList(oecs.getCmd().getCmd()));
    }

    @Test
    public void testGetImage() {
        Assertions.assertEquals("img", oecs.getImage());
    }

    @Test
    public void testEnvs() {
        Map<String, String> envs = oecs.getContainerEnvs();
        Assertions.assertEquals("v", envs.get("k"));
        Assertions.assertEquals("3.9", envs.get("SW_RUNTIME_PYTHON_VERSION"));
        Assertions.assertEquals("0.3.3", envs.get("SW_VERSION"));
        Assertions.assertEquals("rt_n/version/rt_v", envs.get("SW_RUNTIME_VERSION"));
        Assertions.assertEquals("md_n/version/md_v", envs.get("SW_MODEL_VERSION"));
        Assertions.assertEquals("10.2.2.3:8080", envs.get("SW_INSTANCE_URI"));
        Assertions.assertEquals("aabbcc", envs.get("SW_TOKEN"));
        Assertions.assertEquals("p", envs.get("SW_PROJECT"));
        Assertions.assertEquals("", envs.get("SW_PYPI_INDEX_URL"));
        Assertions.assertEquals("", envs.get("SW_PYPI_EXTRA_INDEX_URL"));
        Assertions.assertEquals("", envs.get("SW_PYPI_TRUSTED_HOST"));
        Assertions.assertEquals("0", envs.get("SW_PYPI_TIMEOUT"));
        Assertions.assertEquals("0", envs.get("SW_PYPI_RETRIES"));
        Assertions.assertEquals("1", envs.get("SW_PRODUCTION"));
    }
}
