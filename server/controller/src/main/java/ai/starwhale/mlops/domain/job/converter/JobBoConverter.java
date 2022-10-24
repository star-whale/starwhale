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

import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.mapper.JobSwdsVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
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
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * convert JobEntity to Job
 */

@Slf4j
@Component
public class JobBoConverter {

    final JobSwdsVersionMapper jobSwdsVersionMapper;

    final SwModelPackageMapper swModelPackageMapper;

    final RuntimeMapper runtimeMapper;

    final RuntimeVersionMapper runtimeVersionMapper;

    final SwdsBoConverter swdsBoConverter;

    final SystemSettingService systemSettingService;

    final SwmpVersionConvertor swmpVersionConvertor;

    final StepMapper stepMapper;

    final StepConverter stepConverter;

    final TaskMapper taskMapper;

    final TaskBoConverter taskBoConverter;

    public JobBoConverter(
            JobSwdsVersionMapper jobSwdsVersionMapper,
            SwModelPackageMapper swModelPackageMapper,
            RuntimeMapper runtimeMapper,
            RuntimeVersionMapper runtimeVersionMapper,
            SwdsBoConverter swdsBoConverter,
            SwmpVersionConvertor swmpVersionConvertor,
            SystemSettingService systemSettingService, StepMapper stepMapper,
            StepConverter stepConverter, TaskMapper taskMapper,
            TaskBoConverter taskBoConverter) {
        this.jobSwdsVersionMapper = jobSwdsVersionMapper;
        this.swModelPackageMapper = swModelPackageMapper;
        this.runtimeMapper = runtimeMapper;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.swdsBoConverter = swdsBoConverter;
        this.systemSettingService = systemSettingService;
        this.swmpVersionConvertor = swmpVersionConvertor;
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.taskMapper = taskMapper;
        this.taskBoConverter = taskBoConverter;
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
        String image = runtimeVersionEntity.getImage();
        if (null != systemSettingService.getSystemSetting() && null != systemSettingService.getSystemSetting()
                .getDockerSetting() && null != systemSettingService.getSystemSetting().getDockerSetting()
                .getRegistry()) {
            image = new DockerImage(image).resolve(
                    systemSettingService.getSystemSetting().getDockerSetting().getRegistry());
        }
        Job job = Job.builder()
                .id(jobEntity.getId())
                .project(Project.builder()
                        .id(jobEntity.getProjectId())
                        .name(jobEntity.getProject().getProjectName())
                        .build())
                .jobRuntime(JobRuntime.builder()
                        .name(runtimeEntity.getRuntimeName())
                        .version(runtimeVersionEntity.getVersionName())
                        .storagePath(runtimeVersionEntity.getStoragePath())
                        .image(image)
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
                .resourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()))
                .build();
        return fillStepsAndTasks(job);
    }

    private Job fillStepsAndTasks(Job job) {
        List<StepEntity> stepEntities = stepMapper.findByJobId(job.getId());
        List<Step> steps = stepEntities.parallelStream().map(stepConverter::fromEntity)
                .peek(step -> {
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
