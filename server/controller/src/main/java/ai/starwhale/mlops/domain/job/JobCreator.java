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
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobCreateRequest;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.UserJobConverter;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.upgrade.rollup.aspectcut.WriteOperation;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Component
@Slf4j
public class JobCreator {
    private final JobSpliterator jobSpliterator;
    private final JobLoader jobLoader;
    private final StoragePathCoordinator storagePathCoordinator;
    private final JobDao jobDao;
    private final JobUpdateHelper jobUpdateHelper;

    private final SystemSettingService systemSettingService;
    private final JobSpecParser jobSpecParser;
    private final UserJobConverter userJobConverter;

    public JobCreator(
            JobSpliterator jobSpliterator,
            JobLoader jobLoader,
            StoragePathCoordinator storagePathCoordinator,
            JobDao jobDao,
            JobUpdateHelper jobUpdateHelper,
            SystemSettingService systemSettingService,
            JobSpecParser jobSpecParser,
            UserJobConverter userJobConverter
    ) {
        this.jobSpliterator = jobSpliterator;
        this.jobLoader = jobLoader;
        this.storagePathCoordinator = storagePathCoordinator;
        this.jobDao = jobDao;
        this.jobUpdateHelper = jobUpdateHelper;
        this.systemSettingService = systemSettingService;
        this.jobSpecParser = jobSpecParser;
        this.userJobConverter = userJobConverter;
    }

    @FunctionalInterface
    public interface JobCreationLifeCycle {
        void afterCreation(JobFlattenEntity entity);
    }

    @Transactional
    @WriteOperation
    public Job createJob(JobCreateRequest request, JobCreationLifeCycle jobCreationLifeCycle) {
        String jobUuid = IdUtil.simpleUUID();

        JobFlattenEntity.JobFlattenEntityBuilder builder;
        if (request instanceof UserJobCreateRequest) {
            builder = userJobConverter.convert((UserJobCreateRequest) request);
        } else {
            builder = JobFlattenEntity.builder();
        }

        var stepSpecOverWrites = request.getStepSpecOverWrites();
        var handler = request.getHandler();
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
                    : jobSpecParser.parseStepFromYaml(builder.build().getModelVersion().getJobs(), handler);
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

        var pool = systemSettingService.queryResourcePool(request.getResourcePool());
        if (pool != null) {
            for (var step : steps) {
                pool.validateResources(step.getResources());
            }
            if (!pool.allowUser(request.getUser().getId())) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.JOB, "creator is not allowed to use this resource pool"),
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        JobFlattenEntity jobEntity = builder
                .jobUuid(jobUuid)
                .project(request.getProject())
                .projectId(request.getProject().getId())
                .ownerId(request.getUser().getId())
                .name(steps.get(0).getJobName())
                .ownerName(request.getUser().getName())
                .comment(request.getComment())
                .resultOutputPath(storagePathCoordinator.allocateResultMetricsPath(jobUuid))
                .jobStatus(JobStatus.CREATED)
                .type(request.getJobType())
                .resourcePool(request.getResourcePool())
                .stepSpec(stepSpecOverWrites)
                .createdTime(new Date())
                .modifiedTime(new Date())
                .build();

        jobDao.addJob(jobEntity);
        jobCreationLifeCycle.afterCreation(jobEntity);
        var jobId = jobEntity.getId();
        log.info("Job has been created. ID={}", jobId);

        var job = jobDao.findJobById(jobId);
        jobSpliterator.split(job);
        job = jobDao.findJobById(jobId); // fill steps and tasks
        jobLoader.load(job, false);
        jobUpdateHelper.updateJob(job);

        return job;
    }

}
