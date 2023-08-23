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

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobDatasetVersionMapper;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class JobDao implements BundleAccessor, RecoverAccessor {

    private final JobRepo jobRepo;
    private final JobMapper jobMapper;
    private final JobDatasetVersionMapper datasetVersionMapper;
    private final IdConverter idConvertor;
    private final JobBoConverter jobBoConverter;

    public JobDao(JobRepo jobRepo,
            JobMapper jobMapper,
            JobDatasetVersionMapper datasetVersionMapper,
            IdConverter idConvertor,
            JobBoConverter jobBoConverter) {
        this.jobRepo = jobRepo;
        this.jobMapper = jobMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.idConvertor = idConvertor;
        this.jobBoConverter = jobBoConverter;
    }

    public Long getJobId(String jobUrl) {
        if (idConvertor.isId(jobUrl)) {
            return idConvertor.revert(jobUrl);
        }
        JobEntity jobEntity = jobMapper.findJobByUuid(jobUrl);
        if (jobEntity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(SwValidationException.ValidSubject.JOB,
                            String.format("Unable to find job %s", jobUrl)),
                    HttpStatus.BAD_REQUEST);
        }
        return jobEntity.getId();
    }

    public boolean addJob(JobFlattenEntity jobFlattenEntity) {
        var entity = convertFromFlatten(jobFlattenEntity);
        if (jobMapper.addJob(entity) > 0) {
            // update id
            jobFlattenEntity.setId(entity.getId());
            // add rel
            Set<Long> datasetVersionIds = jobFlattenEntity.getDatasetIdVersionMap().keySet();
            if (!CollectionUtils.isEmpty(datasetVersionIds)) {
                datasetVersionMapper.insert(jobFlattenEntity.getId(), datasetVersionIds);
            }
            // TODO: only sync evaluation repo
            if (jobFlattenEntity.getType() == JobType.EVALUATION) {
                return jobRepo.addJob(jobFlattenEntity) > 0;
            }
            return true;

        }
        return false;
    }

    private JobEntity convertFromFlatten(JobFlattenEntity flattenEntity) {
        return JobEntity.builder()
                .jobUuid(flattenEntity.getJobUuid())
                .ownerId(flattenEntity.getOwnerId())
                .runtimeVersionId(flattenEntity.getRuntimeVersionId())
                .projectId(flattenEntity.getProjectId())
                .modelVersionId(flattenEntity.getModelVersionId())
                .modelName(flattenEntity.getModelName())
                .comment(flattenEntity.getComment())
                .resultOutputPath(flattenEntity.getResultOutputPath())
                .jobStatus(flattenEntity.getJobStatus())
                .type(flattenEntity.getType())
                .resourcePool(flattenEntity.getResourcePool())
                .stepSpec(flattenEntity.getStepSpec())
                .createdTime(flattenEntity.getCreatedTime())
                .modifiedTime(flattenEntity.getModifiedTime())
                .devMode(flattenEntity.isDevMode())
                .devWay(flattenEntity.getDevWay())
                .devPassword(flattenEntity.getDevPassword())
                .autoReleaseTime(flattenEntity.getAutoReleaseTime())
                .virtualJobName(flattenEntity.getName())
                .build();
    }


    public List<JobEntity> listJobs(Long projectId, Long modelId) {
        return jobMapper.listUserJobs(projectId, modelId);
    }

    public List<Job> findJobByStatusIn(List<JobStatus> jobStatuses) {
        return jobMapper.findJobByStatusIn(jobStatuses)
                .stream()
                .map(jobBoConverter::fromEntity)
                .collect(Collectors.toList());
    }

    public Job findJobById(Long jobId) {
        return jobBoConverter.fromEntity(jobMapper.findJobById(jobId));
    }

    public void updateJobStatus(Long jobId, JobStatus jobStatus) {
        jobMapper.updateJobStatus(List.of(jobId), jobStatus);
        jobRepo.updateJobStatus(jobId, jobStatus);
    }

    public void updateJobFinishedTime(Long jobId, Date finishedTime, Long duration) {
        jobMapper.updateJobFinishedTime(List.of(jobId), finishedTime, duration);

        jobRepo.updateJobFinishedTime(jobId, finishedTime, duration);
    }

    public boolean updateJobComment(Long jobId, String comment) {
        return jobMapper.updateJobComment(jobId, comment) > 0 && jobRepo.updateJobComment(jobId, comment) > 0;
    }

    public boolean updateJobComment(String jobUrl, String comment) {
        if (idConvertor.isId(jobUrl)) {
            return this.updateJobComment(idConvertor.revert(jobUrl), comment);
        } else {
            return this.updateJobCommentByUuid(jobUrl, comment);
        }
    }

    public boolean updateJobCommentByUuid(String uuid, String comment) {
        return jobMapper.updateJobCommentByUuid(uuid, comment) > 0 && jobRepo.updateJobCommentByUuid(uuid, comment) > 0;
    }

    public boolean removeJob(Long jobId) {
        return jobMapper.removeJob(jobId) > 0 && jobRepo.removeJob(jobId) > 0;
    }

    public boolean removeJobByUuid(String uuid) {
        return jobMapper.removeJobByUuid(uuid) > 0 && jobRepo.removeJobByUuid(uuid) > 0;
    }

    public Job findJob(String jobUrl) {
        if (idConvertor.isId(jobUrl)) {
            return jobBoConverter.fromEntity(jobMapper.findJobById(idConvertor.revert(jobUrl)));
        } else {
            return jobBoConverter.fromEntity(jobMapper.findJobByUuid(jobUrl));
        }
    }

    public JobEntity findJobEntity(String jobUrl) {
        if (idConvertor.isId(jobUrl)) {
            return jobMapper.findJobById(idConvertor.revert(jobUrl));
        } else {
            return jobMapper.findJobByUuid(jobUrl);
        }
    }

    public boolean updateJobPinStatus(String jobUrl, boolean pinned) {
        Date pinnedTime = pinned ? Date.from(Instant.now()) : null;

        if (idConvertor.isId(jobUrl)) {
            return jobMapper.updateJobPinStatus(idConvertor.revert(jobUrl), pinnedTime) > 0;
        } else {
            return jobMapper.updateJobPinStatusByUuid(jobUrl, pinnedTime) > 0;
        }
    }

    @Override
    public BundleEntity findById(Long id) {
        return jobMapper.findJobById(id);
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return jobMapper.findJobByUuid(name);
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return jobMapper.findJobById(id);
    }


    @Override
    public Boolean recover(Long id) {
        return jobMapper.recoverJob(id) > 0 && jobRepo.recoverJob(id) > 0;
    }

    @Override
    public Type getType() {
        return Type.JOB;
    }
}
