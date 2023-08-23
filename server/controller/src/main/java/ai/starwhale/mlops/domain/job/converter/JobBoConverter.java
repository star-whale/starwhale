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
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.model.mapper.ModelMapper;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeMapper;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

    final StepMapper stepMapper;

    final StepConverter stepConverter;

    final TaskMapper taskMapper;

    final TaskBoConverter taskBoConverter;

    public JobBoConverter(
            DatasetDao datasetDao,
            ModelMapper modelMapper,
            RuntimeMapper runtimeMapper,
            RuntimeVersionMapper runtimeVersionMapper,
            DatasetBoConverter datasetBoConverter,
            JobSpecParser jobSpecParser,
            SystemSettingService systemSettingService, StepMapper stepMapper,
            StepConverter stepConverter, TaskMapper taskMapper,
            TaskBoConverter taskBoConverter) {
        this.datasetDao = datasetDao;
        this.modelMapper = modelMapper;
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.datasetBoConverter = datasetBoConverter;
        this.systemSettingService = systemSettingService;
        this.jobSpecParser = jobSpecParser;
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.taskMapper = taskMapper;
        this.taskBoConverter = taskBoConverter;
    }

    public Job fromEntity(JobEntity jobEntity) {
        List<DataSet> dataSets = datasetDao.listDatasetVersionsOfJob(jobEntity.getId())
                .stream().map(datasetBoConverter::fromVersion)
                .collect(Collectors.toList());
        Model model = modelFromJob(jobEntity);
        JobRuntime jobRuntime = runtimeFromJob(jobEntity);
        Job job = Job.builder()
                .id(jobEntity.getId())
                .uuid(jobEntity.getJobUuid())
                .project(Project.builder()
                        .id(jobEntity.getProjectId())
                        .name(jobEntity.getProject().getProjectName())
                        .build())
                .jobRuntime(jobRuntime)
                .status(jobEntity.getJobStatus())
                .type(jobEntity.getType())
                .model(model)
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
                .virtualJobName(jobEntity.getVirtualJobName())
                .build();
        return fillStepsAndTasks(job);
    }

    private Model modelFromJob(JobEntity jobEntity) {

        if (null != jobEntity.getModelVersion() && StringUtils.hasText(
                jobEntity.getModelVersion().getJobs())) { // virtual jobs use no swcli models
            List<StepSpec> stepSpecs;
            try {
                stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(jobEntity.getModelVersion().getJobs());
            } catch (JsonProcessingException e) {
                log.error("error while parsing job stepSpec, controller version not compile with database data??", e);
                throw new SwValidationException(ValidSubject.JOB, e.getMessage());
            }
            ModelEntity modelEntity = modelMapper.find(
                    jobEntity.getModelVersion().getModelId());
            Model model = Model
                    .builder()
                    .id(jobEntity.getModelVersionId())
                    .name(modelEntity.getModelName())
                    .version(jobEntity.getModelVersion().getVersionName())
                    .projectId(modelEntity.getProjectId())
                    .stepSpecs(stepSpecs)
                    .build();
            return model;
        } else {
            List<StepSpec> stepSpecs;
            try {
                stepSpecs = jobSpecParser.parseAndFlattenStepFromYaml(jobEntity.getStepSpec());
            } catch (JsonProcessingException e) {
                log.error("error while parsing job stepSpec, controller version not compile with database data??", e);
                throw new SwValidationException(ValidSubject.JOB, e.getMessage());
            }
            return Model
                    .builder()
                    .id(-1L)
                    .name(jobEntity.getVirtualJobName())
                    .projectId(jobEntity.getProjectId())
                    .stepSpecs(stepSpecs)
                    .build();
        }

    }

    private JobRuntime runtimeFromJob(JobEntity jobEntity) {
        RuntimeVersionEntity runtimeVersionEntity = runtimeVersionMapper.find(
                jobEntity.getRuntimeVersionId());
        if (null == runtimeVersionEntity) {
            return null; // virtual jobs use no swcli runtimes
        }
        RuntimeEntity runtimeEntity = runtimeMapper.find(
                runtimeVersionEntity.getRuntimeId());
        String builtImage = runtimeVersionEntity.getBuiltImage();
        String image = StringUtils.hasText(builtImage) ? builtImage : runtimeVersionEntity.getImage(
                systemSettingService.getSystemSetting().getDockerSetting().getRegistryForPull());
        JobRuntime jobRuntime = JobRuntime.builder()
                .id(runtimeVersionEntity.getId())
                .name(runtimeEntity.getRuntimeName())
                .version(runtimeVersionEntity.getVersionName())
                .projectId(runtimeEntity.getProjectId())
                .manifest(runtimeVersionEntity.getVersionMetaObj())
                .image(image)
                .build();
        return jobRuntime;
    }

    public Job fillStepsAndTasks(Job job) {
        List<StepEntity> stepEntities = stepMapper.findByJobId(job.getId());
        List<Step> steps = stepEntities.stream().map(entity -> {
            try {
                var step = stepConverter.fromEntity(entity);
                if (step.getResourcePool() == null) {
                    // backward compatibility
                    step.setResourcePool(job.getResourcePool());
                }
                step.setSpec(job.getModel().specOfStep(step.getName()).orElseThrow());
                return step;
            } catch (IOException e) {
                log.error("can not convert step entity to step", e);
                return null;
            }
        }).filter(Objects::nonNull).peek(step -> {
            step.setJob(job);
            List<TaskEntity> taskEntities = taskMapper.findByStepId(step.getId());
            List<Task> tasks = taskBoConverter.fromTaskEntity(taskEntities, step);
            step.setTasks(tasks);
            if (step.getStatus() == StepStatus.RUNNING) {
                if (job.getCurrentStep() != null) {
                    log.error("ERROR!!!!! A job has two running steps job id: {}", job.getId());
                }
                job.setCurrentStep(step);
            }
        }).collect(Collectors.toList());
        linkSteps(steps, stepEntities);
        job.setSteps(steps);
        return job;
    }

    private void linkSteps(List<Step> steps, List<StepEntity> stepEntities) {
        Map<Long, Step> stepMap = steps.parallelStream()
                .collect(Collectors.toMap(Step::getId, Function.identity()));
        Map<Long, Long> linkMap = stepEntities.parallelStream()
                .filter(stepEntity -> null != stepEntity.getLastStepId())
                .collect(Collectors.toMap(StepEntity::getLastStepId, StepEntity::getId));
        steps.forEach(step -> {
            Long nextStepId = linkMap.get(step.getId());
            if (null == nextStepId) {
                return;
            }
            step.setNextStep(stepMap.get(nextStepId));
        });
    }

}
