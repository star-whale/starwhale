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

package ai.starwhale.mlops.domain.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.converter.DatasetBoConverter;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeTestConstants;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.mapper.SystemSettingMapper;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for {@link JobBoConverter}
 */
public class JobBoConverterTest {

    final DatasetBoConverter converter = mock(DatasetBoConverter.class);

    @Test
    public void testJobBoConverter() throws IOException {

        JobEntity jobEntity = JobEntity.builder()
                .id(1L)
                .projectId(1L)
                .project(ProjectEntity.builder().id(1L).projectName("test-project").build())
                .jobStatus(JobStatus.RUNNING)
                .type(JobType.EVALUATION)
                .modelVersionId(1L)
                .modelVersion(ModelVersionEntity.builder().id(1L).modelId(1L).versionName("modelvname")
                        .jobs("stepspec").build())
                .resultOutputPath("job_result")
                .jobUuid(UUID.randomUUID().toString())
                .runtimeVersionId(1L)
                .resourcePool("fool")
                .owner(UserEntity.builder().userName("naf").id(1232L).build())
                .build();
        DatasetDao datasetDao = mock(DatasetDao.class);
        when(datasetDao.listDatasetVersionsOfJob(jobEntity.getId())).thenReturn(List.of(
                DatasetVersion.builder().id(1L).storagePath("path_swds").versionMeta("version_swds")
                        .versionName("name_swds").build(),
                DatasetVersion.builder().id(2L).storagePath("path_swds1").versionMeta("version_swds1")
                        .versionName("name_swds1").build()
        ));

        ModelMapper modelMapper = mock(ModelMapper.class);
        ModelEntity modelEntity = ModelEntity.builder().modelName("name_model")
                .build();
        when(modelMapper.find(
                jobEntity.getModelVersion().getModelId())).thenReturn(modelEntity);

        RuntimeVersionMapper runtimeVersionMapper = mock(RuntimeVersionMapper.class);
        RuntimeVersionEntity runtimeVersionEntity = RuntimeVersionEntity.builder()
                .versionName("name_swrt_version")
                .runtimeId(1L)
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .storagePath("swrt_path")
                .build();
        when(runtimeVersionMapper.find(
                jobEntity.getRuntimeVersionId())).thenReturn(runtimeVersionEntity);

        RuntimeMapper runtimeMapper = mock(RuntimeMapper.class);
        RuntimeEntity runtimeEntity = RuntimeEntity.builder().runtimeName("name_swrt").build();
        when(runtimeMapper.find(
                runtimeVersionEntity.getRuntimeId())).thenReturn(runtimeEntity);

        JobSpecParser jobSpecParser = mock(JobSpecParser.class);
        when(jobSpecParser.parseAndFlattenStepFromYaml(any()))
                .thenReturn(List.of(StepSpec.builder().name("step_name").build()));
        StepConverter stepConverter = mock(StepConverter.class);
        given(stepConverter.fromEntity(any()))
                .willAnswer(invocation -> {
                    StepEntity entity = invocation.getArgument(0);
                    return Step.builder().id(entity.getId()).name("step_name").status(entity.getStatus()).build();
                });
        TaskBoConverter taskBoConverter = mock(TaskBoConverter.class);
        when(taskBoConverter.fromTaskEntity(anyList(), any())).thenReturn(List.of());
        StepMapper stepMapper = mock(StepMapper.class);
        when(stepMapper.findByJobId(jobEntity.getId()))
                .thenReturn(List.of(
                        StepEntity.builder()
                                .id(1L)
                                .status(StepStatus.RUNNING)
                                .build(),
                        StepEntity.builder()
                                .id(2L)
                                .lastStepId(1L).build()
                ));
        TaskMapper taskMapper = mock(TaskMapper.class);
        when(taskMapper.findByStepId(any())).thenReturn(
                List.of(TaskEntity.builder().build(), TaskEntity.builder().build()));

        SystemSettingService systemSettingService = new SystemSettingService(
                mock(SystemSettingMapper.class), List.of(), null, new DockerSetting(), null);
        systemSettingService.updateSetting("---\n"
                + "dockerSetting:\n"
                + "  registryForPull: \"\"\n"
                + "  registryForPush: \"\"\n"
                + "  userName: \"\"\n"
                + "  password: \"\"\n"
                + "  insecure: true\n"
                + "resourcePoolSetting:\n"
                + "- name: \"fool\"\n"
                + "  nodeSelector: \n"
                + "    foo: \"bar\"\n"
                + "  resources:\n"
                + "  - name: \"cpu\"\n"
                + "    max: null\n"
                + "    min: null\n"
                + "    defaults: 5.0");
        JobBoConverter jobBoConverter = new JobBoConverter(datasetDao, modelMapper, runtimeMapper,
                runtimeVersionMapper,
                converter,
                jobSpecParser, systemSettingService,
                stepMapper, stepConverter, taskMapper, taskBoConverter);

        Job job = jobBoConverter.fromEntity(jobEntity);
        Assertions.assertEquals(jobEntity.getJobStatus(), job.getStatus());
        Assertions.assertEquals(jobEntity.getId(), job.getId());
        Assertions.assertEquals(jobEntity.getType(), job.getType());
        Assertions.assertEquals(jobEntity.getResultOutputPath(), job.getOutputDir());
        JobRuntime swrt = job.getJobRuntime();
        Assertions.assertNotNull(swrt);
        Assertions.assertEquals(runtimeVersionEntity.getVersionName(), swrt.getVersion());
        Assertions.assertEquals(runtimeEntity.getRuntimeName(), swrt.getName());

        Model model = job.getModel();
        Assertions.assertNotNull(model);
        Assertions.assertEquals(jobEntity.getModelVersion().getVersionName(), model.getVersion());
        Assertions.assertEquals(jobEntity.getModelVersion().getId(), model.getId());
        Assertions.assertEquals(modelEntity.getModelName(), model.getName());
        Assertions.assertEquals(List.of(StepSpec.builder().name("step_name").concurrency(null).build()),
                model.getStepSpecs());

        List<DataSet> dataSets = job.getDataSets();
        Assertions.assertNotNull(dataSets);
        Assertions.assertEquals(2, dataSets.size());

        Assertions.assertEquals("fool", job.getResourcePool().getName());

        Assertions.assertEquals(1L, job.getCurrentStep().getId());
        Assertions.assertEquals(2L, job.getCurrentStep().getNextStep().getId());
        Assertions.assertEquals(2, job.getSteps().size());

        Assertions.assertEquals(jobEntity.getOwner().getId(), job.getOwner().getId());
        Assertions.assertEquals(jobEntity.getOwner().getUserName(), job.getOwner().getName());
    }

}
