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

package ai.starwhale.mlops.domain.job.converter;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.mapper.JobSwdsVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.swds.bo.SwDataSet;
import ai.starwhale.mlops.domain.swds.converter.SwdsBoConverter;
import ai.starwhale.mlops.domain.swmp.SwModelPackage;
import ai.starwhale.mlops.domain.swmp.SwmpVersionConvertor;
import ai.starwhale.mlops.domain.swmp.mapper.SwModelPackageMapper;
import ai.starwhale.mlops.domain.swmp.po.SwModelPackageEntity;
import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolConverter;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * convert JobEntity to Job
 */
@Component
public class JobBoConverter {

    final JobSwdsVersionMapper jobSwdsVersionMapper;

    final SwModelPackageMapper swModelPackageMapper;

    final RuntimeMapper runtimeMapper;

    final RuntimeVersionMapper runtimeVersionMapper;

    final ResourcePoolMapper resourcePoolMapper;

    final ResourcePoolConverter resourcePoolConverter;

    final SwdsBoConverter swdsBoConverter;

    final String defaultRuntimeImage;

    final SwmpVersionConvertor swmpVersionConvertor;

    public JobBoConverter(
            JobSwdsVersionMapper jobSwdsVersionMapper,
            SwModelPackageMapper swModelPackageMapper,
            RuntimeMapper runtimeMapper,
            RuntimeVersionMapper runtimeVersionMapper,
            SwdsBoConverter swdsBoConverter,
            @Value("${sw.runtime.image-default}") String defaultImage,
            ResourcePoolMapper resourcePoolMapper,
            ResourcePoolConverter resourcePoolConverter,
            SwmpVersionConvertor swmpVersionConvertor) {
        this.jobSwdsVersionMapper = jobSwdsVersionMapper;
        this.swModelPackageMapper = swModelPackageMapper;
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.swdsBoConverter = swdsBoConverter;
        this.defaultRuntimeImage = defaultImage;
        this.resourcePoolMapper = resourcePoolMapper;
        this.resourcePoolConverter = resourcePoolConverter;
        this.swmpVersionConvertor = swmpVersionConvertor;
    }

    public Job fromEntity(JobEntity jobEntity) {
        List<SwDataSet> swDataSets = jobSwdsVersionMapper.listSwdsVersionsByJobId(jobEntity.getId())
                .stream().map(swdsBoConverter::fromEntity)
                .collect(Collectors.toList());
        SwModelPackageEntity modelPackageEntity = swModelPackageMapper.findSwModelPackageById(
                jobEntity.getSwmpVersion().getSwmpId());
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.findVersionById(
                jobEntity.getRuntimeVersionId());
        RuntimeEntity runtimeEntity = runtimeMapper.findRuntimeById(
                runtimeVersionEntity.getRuntimeId());
        ResourcePoolEntity resourcePoolEntity = resourcePoolMapper.findById(jobEntity.getResourcePoolId());
        ResourcePool resourcePool = resourcePoolConverter.toResourcePool(resourcePoolEntity);
        return Job.builder()
                .id(jobEntity.getId())
                .project(Project.builder()
                        .id(jobEntity.getProjectId())
                        .name(jobEntity.getProject().getProjectName())
                        .build())
                .jobRuntime(JobRuntime.builder()
                        .name(runtimeEntity.getRuntimeName())
                        .version(runtimeVersionEntity.getVersionName())
                        .storagePath(runtimeVersionEntity.getStoragePath())
                        .image(null == runtimeVersionEntity.getImage() ? defaultRuntimeImage
                                : runtimeVersionEntity.getImage())
                        .build())
                .status(jobEntity.getJobStatus())
                .type(jobEntity.getType())
                .swmp(SwModelPackage
                        .builder()
                        .id(jobEntity.getSwmpVersionId())
                        .name(modelPackageEntity.getSwmpName())
                        .version(jobEntity.getSwmpVersion().getVersionName())
                        .path(jobEntity.getSwmpVersion().getStoragePath())
                        .stepSpecs(swmpVersionConvertor.convert(jobEntity.getSwmpVersion()).getStepSpecs())
                        .build()
                )
                .stepSpec(jobEntity.getStepSpec())
                .swDataSets(swDataSets)
                .outputDir(jobEntity.getResultOutputPath())
                .uuid(jobEntity.getJobUuid())
                .resourcePool(resourcePool)
                .build();
    }

}
