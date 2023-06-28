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

import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.dataset.converter.DatasetBoConverter;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.step.StepService;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * convert JobEntity to Job
 */

@Slf4j
@Component
public class JobBoConverter {

    final DatasetDao datasetDao;

    final ModelMapper modelMapper;

    final RuntimeMapper runtimeMapper;

    final RuntimeVersionMapper runtimeVersionMapper;

    final DatasetBoConverter datasetBoConverter;

    final SystemSettingService systemSettingService;

    final JobSpecParser jobSpecParser;

    final StepService stepService;

    public JobBoConverter(
            DatasetDao datasetDao,
            ModelMapper modelMapper,
            RuntimeMapper runtimeMapper,
            RuntimeVersionMapper runtimeVersionMapper,
            DatasetBoConverter datasetBoConverter,
            JobSpecParser jobSpecParser,
            SystemSettingService systemSettingService,
            StepService stepService) {
        this.datasetDao = datasetDao;
        this.modelMapper = modelMapper;
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.datasetBoConverter = datasetBoConverter;
        this.systemSettingService = systemSettingService;
        this.jobSpecParser = jobSpecParser;
        this.stepService = stepService;
    }

    public Job fromEntity(JobEntity jobEntity) {
        List<DataSet> dataSets = datasetDao.listDatasetVersionsOfJob(jobEntity.getId())
                .stream().map(datasetBoConverter::fromVersion)
                .collect(Collectors.toList());
        ModelEntity modelEntity = modelMapper.find(
                jobEntity.getModelVersion().getModelId());
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.find(
                jobEntity.getRuntimeVersionId());
        RuntimeEntity runtimeEntity = runtimeMapper.find(
                runtimeVersionEntity.getRuntimeId());
        String builtImage = runtimeVersionEntity.getBuiltImage();
        String image = StringUtils.hasText(builtImage) ? builtImage : runtimeVersionEntity.getImage(
                    systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPull());

        Job job;
        try {
            job = Job.builder()
                    .id(jobEntity.getId())
                    .uuid(jobEntity.getJobUuid())
                    .project(Project.builder()
                            .id(jobEntity.getProjectId())
                            .name(jobEntity.getProject().getProjectName())
                            .build())
                    .jobRuntime(JobRuntime.builder()
                            .id(runtimeVersionEntity.getId())
                            .name(runtimeEntity.getRuntimeName())
                            .version(runtimeVersionEntity.getVersionName())
                            .storagePath(runtimeVersionEntity.getStoragePath())
                            .image(image)
                            .build())
                    .status(jobEntity.getJobStatus())
                    .type(jobEntity.getType())
                    .model(Model
                            .builder()
                            .id(jobEntity.getModelVersionId())
                            .name(modelEntity.getModelName())
                            .version(jobEntity.getModelVersion().getVersionName())
                            .stepSpecs(jobSpecParser.parseAndFlattenStepFromYaml(jobEntity.getModelVersion().getJobs()))
                            .build()
                    )
                    .stepSpec(jobEntity.getStepSpec())
                    .dataSets(dataSets)
                    .outputDir(jobEntity.getResultOutputPath())
                    .resourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()))
                    .owner(User.builder()
                            .id(jobEntity.getOwner().getId())
                            .name(jobEntity.getOwner().getUserName())
                            .createdTime(jobEntity.getOwner().getCreatedTime())
                            .build())
                    .createdTime(jobEntity.getCreatedTime())
                    .finishedTime(jobEntity.getFinishedTime())
                    .durationMs(jobEntity.getDurationMs())
                    .comment(jobEntity.getComment())
                    .devMode(jobEntity.isDevMode())
                    .devWay(jobEntity.getDevWay())
                    .devPassword(jobEntity.getDevPassword())
                    .autoReleaseTime(jobEntity.getAutoReleaseTime())
                    .pinnedTime(jobEntity.getPinnedTime())
                    .build();
        } catch (JsonProcessingException e) {
            throw new SwValidationException(ValidSubject.JOB, e.getMessage());
        }
        return fillStepsAndTasks(job);
    }

    public Job fillStepsAndTasks(Job job) {
        stepService.fillJobSteps(job);
        return job;
    }

}
