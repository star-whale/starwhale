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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobSwdsVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.bo.SwDataSet;
import ai.starwhale.mlops.domain.swds.converter.SwdsBoConverter;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.swmp.SwModelPackage;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolConverter;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.storage.StorageAccessService;
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
                .deviceAmount(1)
                .deviceType(Clazz.CPU.getValue())
                .jobStatus(JobStatus.RUNNING)
                .type(JobType.EVALUATION)
                .swmpVersionId(1L)
                .swmpVersion(SwModelPackageVersionEntity.builder().id(1L).swmpId(1L).versionName("swmpvname")
                        .storagePath("swmp_path").build())
                .resultOutputPath("job_result")
                .jobUuid(UUID.randomUUID().toString())
                .runtimeVersionId(1L)
                .resourcePoolId(7L)
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

        ResourcePoolMapper resourcePoolMapper = mock(ResourcePoolMapper.class);
        ResourcePoolEntity resourcePoolEntity = ResourcePoolEntity.builder().id(7L).label("foo").build();
        when(resourcePoolMapper.findById(7L)).thenReturn(resourcePoolEntity);
        ResourcePoolConverter resourcePoolConverter = mock(ResourcePoolConverter.class);
        ResourcePool resourcePool = ResourcePool.builder().label(resourcePoolEntity.getLabel()).build();
        when(resourcePoolConverter.toResourcePool(resourcePoolEntity)).thenReturn(resourcePool);

        JobBoConverter jobBoConverter = new JobBoConverter(jobSwdsVersionMapper, swModelPackageMapper, runtimeMapper,
                runtimeVersionMapper,
                swdsboConverter, resourcePoolMapper, resourcePoolConverter,
                new SystemSettingService(new YAMLMapper(), new StoragePathCoordinator("test"),
                        mock(StorageAccessService.class)));

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
        Assertions.assertEquals(jobEntity.getDeviceAmount(), swrt.getDeviceAmount());
        Assertions.assertEquals(jobEntity.getDeviceType(), swrt.getDeviceClass().getValue());

        SwModelPackage swmp = job.getSwmp();
        Assertions.assertNotNull(swmp);
        Assertions.assertEquals(jobEntity.getSwmpVersion().getVersionName(), swmp.getVersion());
        Assertions.assertEquals(jobEntity.getSwmpVersion().getId(), swmp.getId());
        Assertions.assertEquals(swModelPackageEntity.getSwmpName(), swmp.getName());
        Assertions.assertEquals(jobEntity.getSwmpVersion().getStoragePath(), swmp.getPath());

        List<SwDataSet> swDataSets = job.getSwDataSets();
        Assertions.assertNotNull(swDataSets);
        Assertions.assertEquals(2, swDataSets.size());

        Assertions.assertEquals(jobEntity.getResourcePoolId(), resourcePoolEntity.getId());
        Assertions.assertEquals(job.getResourcePool(), resourcePool);
    }

}
