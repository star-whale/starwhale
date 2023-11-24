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
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.upgrade.rollup.aspectcut.WriteOperation;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Slf4j
public class JobCreator {
    private final JobSpliterator jobSpliterator;
    private final JobLoader jobLoader;
    private final StoragePathCoordinator storagePathCoordinator;
    private final JobDao jobDao;
    private final JobUpdateHelper jobUpdateHelper;

    private final UserJobConverter userJobConverter;

    public JobCreator(
            JobSpliterator jobSpliterator,
            JobLoader jobLoader,
            StoragePathCoordinator storagePathCoordinator,
            JobDao jobDao,
            JobUpdateHelper jobUpdateHelper,
            UserJobConverter userJobConverter
    ) {
        this.jobSpliterator = jobSpliterator;
        this.jobLoader = jobLoader;
        this.storagePathCoordinator = storagePathCoordinator;
        this.jobDao = jobDao;
        this.jobUpdateHelper = jobUpdateHelper;
        this.userJobConverter = userJobConverter;
    }

    @Transactional
    @WriteOperation
    public Job createJob(JobCreateRequest request) {
        String jobUuid = IdUtil.simpleUUID();

        JobFlattenEntity.JobFlattenEntityBuilder builder;
        if (request instanceof UserJobCreateRequest) {
            builder = userJobConverter.convert((UserJobCreateRequest) request);
        } else {
            String stepSpec;
            try {
                stepSpec = Constants.yamlMapper.writeValueAsString(request.getStepSpecOverWrites());
            } catch (JsonProcessingException e) {
                throw new SwProcessException(ErrorType.SYSTEM, "serialize stepSpec failed", e);
            }
            builder = JobFlattenEntity.builder()
                    .name(request.getStepSpecOverWrites().get(0).getJobName())
                    .stepSpec(stepSpec);
        }

        JobFlattenEntity jobEntity = builder
                .jobUuid(jobUuid)
                .project(request.getProject())
                .projectId(request.getProject().getId())
                .ownerId(request.getUser().getId())
                .ownerName(request.getUser().getName())
                .comment(request.getComment())
                .resultOutputPath(storagePathCoordinator.allocateResultMetricsPath(jobUuid))
                .jobStatus(JobStatus.CREATED)
                .type(request.getJobType())
                .resourcePool(request.getResourcePool())
                .createdTime(new Date())
                .modifiedTime(new Date())
                .build();

        jobDao.addJob(jobEntity);
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
