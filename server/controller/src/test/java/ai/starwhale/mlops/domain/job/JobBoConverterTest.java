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
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
import ai.starwhale.mlops.domain.swds.converter.SWDSBOConverter;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.po.SWModelPackageVersionEntity;
import java.util.List;
import java.util.UUID;

import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolConverter;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for {@link JobBoConverter}
 */
public class JobBoConverterTest {

    final SWDSBOConverter swdsboConverter = mock(SWDSBOConverter.class);

    @Test
    public void testJobBoConverter(){

        JobEntity jobEntity = JobEntity.builder()
            .id(1L)
            .deviceAmount(1)
            .deviceType(Clazz.CPU.getValue())
            .jobStatus(JobStatus.RUNNING)
            .type(JobType.EVALUATION)
            .swmpVersionId(1L)
            .swmpVersion(SWModelPackageVersionEntity.builder().id(1L).swmpId(1L).versionName("swmpvname").storagePath("swmp_path").build())
            .resultOutputPath("job_result")
            .jobUuid(UUID.randomUUID().toString())
            .runtimeVersionId(1L)
            .resourcePoolId(7L)
            .build();
        JobSWDSVersionMapper jobSWDSVersionMapper = mock(JobSWDSVersionMapper.class);
        when(jobSWDSVersionMapper.listSWDSVersionsByJobId(jobEntity.getId())).thenReturn(List.of(
            SWDatasetVersionEntity.builder().id(1L).storagePath("path_swds").versionMeta("version_swds").versionName("name_swds").build()
            ,SWDatasetVersionEntity.builder().id(2L).storagePath("path_swds1").versionMeta("version_swds1").versionName("name_swds1").build()
        ));


        SWModelPackageMapper swModelPackageMapper = mock(SWModelPackageMapper.class);
        SWModelPackageEntity swModelPackageEntity = SWModelPackageEntity.builder().swmpName("name_swmp")
            .build();
        when(swModelPackageMapper.findSWModelPackageById(
            jobEntity.getSwmpVersion().getSwmpId())).thenReturn(swModelPackageEntity);

        RuntimeVersionMapper runtimeVersionMapper = mock(RuntimeVersionMapper.class);
        RuntimeVersionEntity runtimeVersionEntity = RuntimeVersionEntity.builder().versionName("name_swrt_version").runtimeId(1L).storagePath("swrt_path").build();
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


        JobBoConverter jobBoConverter = new JobBoConverter(jobSWDSVersionMapper,swModelPackageMapper,runtimeMapper,runtimeVersionMapper,
            swdsboConverter,"ghcr.io/star-whale/starwhale:latest", resourcePoolMapper, resourcePoolConverter);

        Job job = jobBoConverter.fromEntity(jobEntity);
        Assertions.assertEquals(jobEntity.getJobStatus(),job.getStatus());
        Assertions.assertEquals(jobEntity.getId(),job.getId());
        Assertions.assertEquals(jobEntity.getType(),job.getType());
        Assertions.assertEquals(jobEntity.getResultOutputPath(),job.getResultDir());
        Assertions.assertEquals(jobEntity.getJobUuid(),job.getUuid());
        JobRuntime swrt = job.getJobRuntime();
        Assertions.assertNotNull(swrt);
        Assertions.assertEquals(runtimeVersionEntity.getVersionName(),swrt.getVersion());
        Assertions.assertEquals(runtimeEntity.getRuntimeName(),swrt.getName());
        Assertions.assertEquals(runtimeVersionEntity.getStoragePath(),swrt.getStoragePath());
        Assertions.assertEquals(jobEntity.getDeviceAmount(),swrt.getDeviceAmount());
        Assertions.assertEquals(jobEntity.getDeviceType(),swrt.getDeviceClass().getValue());

        SWModelPackage swmp = job.getSwmp();
        Assertions.assertNotNull(swmp);
        Assertions.assertEquals(jobEntity.getSwmpVersion().getVersionName(),swmp.getVersion());
        Assertions.assertEquals(jobEntity.getSwmpVersion().getId(),swmp.getId());
        Assertions.assertEquals(swModelPackageEntity.getSwmpName(),swmp.getName());
        Assertions.assertEquals(jobEntity.getSwmpVersion().getStoragePath(),swmp.getPath());

        List<SWDataSet> swDataSets = job.getSwDataSets();
        Assertions.assertNotNull(swDataSets);
        Assertions.assertEquals(2, swDataSets.size());

        Assertions.assertEquals(jobEntity.getResourcePoolId(), resourcePoolEntity.getId());
        Assertions.assertEquals(job.getResourcePool(), resourcePool);
    }

}
