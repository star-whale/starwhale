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

import ai.starwhale.mlops.api.protocol.swmp.SwModelPackageVersionVo;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobSwdsVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.swds.bo.SwDataSet;
import ai.starwhale.mlops.domain.swds.converter.SwdsBoConverter;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.swmp.SwModelPackage;
import ai.starwhale.mlops.domain.swmp.SwmpVersionConvertor;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.mapper.SystemSettingMapper;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for {@link JobBoConverter}
 */
public class JobBoConverterTest {

    final SwdsBoConverter swdsboConverter = mock(SwdsBoConverter.class);

    @Test
    public void testJobBoConverter() {

        JobEntity jobEntity = JobEntity.builder()
                .id(1L)
                .projectId(1L)
                .project(ProjectEntity.builder().id(1L).projectName("test-project").build())
                .jobStatus(JobStatus.RUNNING)
                .type(JobType.EVALUATION)
                .swmpVersionId(1L)
                .swmpVersion(SwModelPackageVersionEntity.builder().id(1L).swmpId(1L).versionName("swmpvname")
                        .storagePath("swmp_path").evalJobs("stepspec").build())
                .resultOutputPath("job_result")
                .jobUuid(UUID.randomUUID().toString())
                .runtimeVersionId(1L)
                .resourcePool("rp")
                .build();
        JobSwdsVersionMapper jobSwdsVersionMapper = mock(JobSwdsVersionMapper.class);
        when(jobSwdsVersionMapper.listSwdsVersionsByJobId(jobEntity.getId())).thenReturn(List.of(
                SwDatasetVersionEntity.builder().id(1L).storagePath("path_swds").versionMeta("version_swds")
                        .versionName("name_swds").build(),
                SwDatasetVersionEntity.builder().id(2L).storagePath("path_swds1").versionMeta("version_swds1")
                        .versionName("name_swds1").build()
        ));

        SwModelPackageMapper swModelPackageMapper = mock(SwModelPackageMapper.class);
        SwModelPackageEntity swModelPackageEntity = SwModelPackageEntity.builder().swmpName("name_swmp")
                .build();
        when(swModelPackageMapper.findSwModelPackageById(
                jobEntity.getSwmpVersion().getSwmpId())).thenReturn(swModelPackageEntity);

        RuntimeVersionMapper runtimeVersionMapper = mock(RuntimeVersionMapper.class);
        RuntimeVersionEntity runtimeVersionEntity = RuntimeVersionEntity.builder().versionName("name_swrt_version")
                .runtimeId(1L).storagePath("swrt_path").build();
        when(runtimeVersionMapper.findVersionById(
                jobEntity.getRuntimeVersionId())).thenReturn(runtimeVersionEntity);

        RuntimeMapper runtimeMapper = mock(RuntimeMapper.class);
        RuntimeEntity runtimeEntity = RuntimeEntity.builder().runtimeName("name_swrt").build();
        when(runtimeMapper.findRuntimeById(
                runtimeVersionEntity.getRuntimeId())).thenReturn(runtimeEntity);

        SwmpVersionConvertor swmpVersionConvertor = mock(SwmpVersionConvertor.class);
        when(swmpVersionConvertor.convert(any())).thenReturn(
                SwModelPackageVersionVo.builder().stepSpecs(List.of(new StepSpec())).build());
        StepConverter stepConverter = mock(StepConverter.class);
        given(stepConverter.fromEntity(any()))
                .willAnswer(invocation -> {
                    StepEntity entity = invocation.getArgument(0);
                    return Step.builder().id(entity.getId()).status(entity.getStatus()).build();
                });
        TaskBoConverter taskBoConverter = mock(TaskBoConverter.class);
        when(taskBoConverter.fromTaskEntity(anyList(), any())).thenReturn(List.of());
        StepMapper stepMapper = mock(StepMapper.class);
        when(stepMapper.findByJobId(jobEntity.getId())).thenReturn(List.of(StepEntity.builder().id(1L).status(
                StepStatus.RUNNING).build(), StepEntity.builder().id(2L).lastStepId(1L).build()));
        TaskMapper taskMapper = mock(TaskMapper.class);
        when(taskMapper.findByStepId(any())).thenReturn(
                List.of(TaskEntity.builder().build(), TaskEntity.builder().build()));

        SystemSettingService systemSettingService = mock(SystemSettingService.class);
        when(systemSettingService.queryResourcePool("rp")).thenReturn(ResourcePool.builder().name("fool").build());
        JobBoConverter jobBoConverter = new JobBoConverter(jobSwdsVersionMapper, swModelPackageMapper, runtimeMapper,
                runtimeVersionMapper,
                swdsboConverter,
                swmpVersionConvertor, systemSettingService,
                stepMapper, stepConverter, taskMapper, taskBoConverter);

        Job job = jobBoConverter.fromEntity(jobEntity);
        Assertions.assertEquals(jobEntity.getJobStatus(), job.getStatus());
        Assertions.assertEquals(jobEntity.getId(), job.getId());
        Assertions.assertEquals(jobEntity.getType(), job.getType());
        Assertions.assertEquals(jobEntity.getResultOutputPath(), job.getOutputDir());
        Assertions.assertEquals(jobEntity.getJobUuid(), job.getUuid());
        JobRuntime swrt = job.getJobRuntime();
        Assertions.assertNotNull(swrt);
        Assertions.assertEquals(runtimeVersionEntity.getVersionName(), swrt.getVersion());
        Assertions.assertEquals(runtimeEntity.getRuntimeName(), swrt.getName());
        Assertions.assertEquals(runtimeVersionEntity.getStoragePath(), swrt.getStoragePath());

        SwModelPackage swmp = job.getSwmp();
        Assertions.assertNotNull(swmp);
        Assertions.assertEquals(jobEntity.getSwmpVersion().getVersionName(), swmp.getVersion());
        Assertions.assertEquals(jobEntity.getSwmpVersion().getId(), swmp.getId());
        Assertions.assertEquals(swModelPackageEntity.getSwmpName(), swmp.getName());
        Assertions.assertEquals(jobEntity.getSwmpVersion().getStoragePath(), swmp.getPath());
        Assertions.assertEquals(List.of(new StepSpec()), swmp.getStepSpecs());

        List<SwDataSet> swDataSets = job.getSwDataSets();
        Assertions.assertNotNull(swDataSets);
        Assertions.assertEquals(2, swDataSets.size());

        Assertions.assertEquals("fool", job.getResourcePool().getName());

        Assertions.assertEquals(1L, job.getCurrentStep().getId());
        Assertions.assertEquals(2L, job.getCurrentStep().getNextStep().getId());
        Assertions.assertEquals(2, job.getSteps().size());
    }

}
