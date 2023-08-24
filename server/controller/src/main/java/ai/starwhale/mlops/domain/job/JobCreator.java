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

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.bo.Runtime;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Component
@Slf4j
public class JobCreator {

    private final JobBoConverter jobBoConverter;
    private final JobSpliterator jobSpliterator;
    private final JobLoader jobLoader;
    private final StoragePathCoordinator storagePathCoordinator;
    private final JobDao jobDao;
    private final ModelService modelService;
    private final DatasetDao datasetDao;
    private final RuntimeDao runtimeDao;
    private final JobUpdateHelper jobUpdateHelper;

    private final SystemSettingService systemSettingService;
    private final JobSpecParser jobSpecParser;

    public JobCreator(
            JobBoConverter jobBoConverter,
            JobSpliterator jobSpliterator, JobLoader jobLoader,
            StoragePathCoordinator storagePathCoordinator,
            JobDao jobDao, ModelService modelService,
            DatasetDao datasetDao, RuntimeDao runtimeDao, JobUpdateHelper jobUpdateHelper,
            SystemSettingService systemSettingService, JobSpecParser jobSpecParser
    ) {
        this.jobBoConverter = jobBoConverter;
        this.jobSpliterator = jobSpliterator;
        this.jobLoader = jobLoader;
        this.storagePathCoordinator = storagePathCoordinator;
        this.jobDao = jobDao;
        this.modelService = modelService;
        this.datasetDao = datasetDao;
        this.runtimeDao = runtimeDao;
        this.jobUpdateHelper = jobUpdateHelper;
        this.systemSettingService = systemSettingService;
        this.jobSpecParser = jobSpecParser;
    }


    @Transactional
    public Job createJob(
            Project project,
            String modelVersionUrl,
            String datasetVersionUrls,
            String runtimeVersionUrl,
            String comment,
            String resourcePool,
            String handler,
            String stepSpecOverWrites,
            JobType type,
            DevWay devWay,
            boolean devMode,
            String devPassword,
            Long ttlInSec,
            User creator
    ) {
        String jobUuid = IdUtil.simpleUUID();
        var modelVersion = StringUtils.hasText(modelVersionUrl) ? modelService.findModelVersion(modelVersionUrl) : null;
        var model = null == modelVersion ? null : modelService.findModel(modelVersion.getModelId());

        RuntimeVersion runtimeVersion;
        if (StringUtils.hasText(runtimeVersionUrl)) {
            runtimeVersion = RuntimeVersion.fromEntity(runtimeDao.getRuntimeVersion(runtimeVersionUrl));
        } else if (null != modelVersion) {
            log.debug("try to find built-in runtime for model:{}", modelVersion.getId());
            runtimeVersionUrl = modelVersion.getBuiltInRuntime();
            if (!StringUtils.hasText(runtimeVersionUrl)) {
                throw new SwValidationException(ValidSubject.RUNTIME, "no runtime or built-in runtime");
            }
            var runtime = runtimeDao.getRuntimeByName(Constants.SW_BUILT_IN_RUNTIME, model.getProjectId());
            runtimeVersion = RuntimeVersion.fromEntity(runtimeDao.getRuntimeVersion(
                    runtime.getId(),
                    runtimeVersionUrl
            ));
        } else {
            runtimeVersion = null;
        }
        var runtime = null == runtimeVersion ? null :
                Runtime.fromEntity(runtimeDao.getRuntime(runtimeVersion.getRuntimeId()));

        var datasetVersionIdMaps = StringUtils.hasText(datasetVersionUrls)
                ? Arrays.stream(datasetVersionUrls.split("[,;]"))
                .map(datasetDao::getDatasetVersion)
                .collect(Collectors.toMap(DatasetVersion::getId, DatasetVersion::getVersionName))
                : new HashMap<Long, String>();

        if ((!StringUtils.hasText(stepSpecOverWrites) && !StringUtils.hasText(handler))
                || (StringUtils.hasText(stepSpecOverWrites) && StringUtils.hasText(handler))) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "handler or stepSpec must be provided only one"),
                    HttpStatus.BAD_REQUEST
            );
        }

        List<StepSpec> steps;
        try {
            steps = StringUtils.hasText(stepSpecOverWrites)
                    ? jobSpecParser.parseAndFlattenStepFromYaml(stepSpecOverWrites)
                    : jobSpecParser.parseStepFromYaml(modelVersion.getJobs(), handler);
            for (var s : steps) {
                s.verifyStepSpecArgs();
            }
            stepSpecOverWrites = Constants.yamlMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "failed to parse job step", e), HttpStatus.BAD_REQUEST);
        }

        if (CollectionUtils.isEmpty(steps)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "no stepSpec is configured"), HttpStatus.BAD_REQUEST);
        }

        var pool = systemSettingService.queryResourcePool(resourcePool);
        if (pool != null) {
            for (var step : steps) {
                pool.validateResources(step.getResources());
            }
            if (!pool.allowUser(creator.getId())) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.JOB, "creator is not allowed to use this resource pool"),
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        JobFlattenEntity jobEntity = JobFlattenEntity.builder()
                .jobUuid(jobUuid)
                .ownerId(creator.getId())
                .name(steps.get(0).getJobName())
                .ownerName(creator.getName())
                .runtimeVersionId(null == runtimeVersion ? null : runtimeVersion.getId())
                .runtimeVersionValue(null == runtimeVersion ? null : runtimeVersion.getVersionName())
                .runtimeName(null == runtime ? null : runtime.getName())
                .projectId(project.getId())
                .project(project)
                .modelVersionId(null == modelVersion ? null : modelVersion.getId())
                .modelVersionValue(null == modelVersion ? null : modelVersion.getName())
                .modelName(null == model ? null : model.getName())
                .datasetIdVersionMap(datasetVersionIdMaps)
                .comment(comment)
                .resultOutputPath(storagePathCoordinator.allocateResultMetricsPath(jobUuid))
                .jobStatus(JobStatus.CREATED)
                .type(type)
                .resourcePool(resourcePool)
                .stepSpec(stepSpecOverWrites)
                .createdTime(new Date())
                .modifiedTime(new Date())
                .devMode(devMode)
                .devWay(devMode ? devWay : null)
                .devPassword(devMode ? devPassword : null)
                .autoReleaseTime(ttlInSec == null ? null : new Date(System.currentTimeMillis() + ttlInSec * 1000))
                .build();

        jobDao.addJob(jobEntity);
        var jobId = jobEntity.getId();
        log.info("Job has been created. ID={}", jobId);

        var job = jobDao.findJobById(jobId);
        jobSpliterator.split(job);
        jobBoConverter.fillStepsAndTasks(job);
        jobLoader.load(job, false);
        jobUpdateHelper.updateJob(job);

        return job;
    }

}
