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

import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.impl.DatasetBuildTaskSpecificationFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatasetBuildContainerSpecificationBuilderFactoryTest {

    Task task;

    DatasetBuildTaskSpecificationFactory datasetBuildTaskEntrypointBuilderFactory;

    @BeforeEach
    public void setup() {
        task = mock(Task.class);
        datasetBuildTaskEntrypointBuilderFactory = new DatasetBuildTaskSpecificationFactory(mock(
                SystemSettingService.class), mock(TaskTokenValidator.class), "");
    }

    @Test
    public void testNotMatch() {
        when(task.getStep()).thenReturn(Step.builder()
                .job(Job.builder().project(Project.builder().name("p").build()).virtualJobName("abc").build()).build());
        Assertions.assertFalse(datasetBuildTaskEntrypointBuilderFactory.matches(task));

        when(task.getStep()).thenReturn(Step.builder()
                .job(Job.builder().project(Project.builder().name("p").build()).virtualJobName("").build()).build());
        Assertions.assertFalse(datasetBuildTaskEntrypointBuilderFactory.matches(task));

        when(task.getStep()).thenReturn(Step.builder()
                .job(Job.builder().project(Project.builder().name("p").build()).virtualJobName(null).build()).build());
        Assertions.assertFalse(datasetBuildTaskEntrypointBuilderFactory.matches(task));
    }

}
