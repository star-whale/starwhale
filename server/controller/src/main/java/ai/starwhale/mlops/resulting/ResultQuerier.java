/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.api.protocol.resulting.EvaluationResult;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.resulting.repo.IndicatorRepo;
import ai.starwhale.mlops.resulting.repo.IndicatorRepoFinder;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * coordinate collectors of jobs
 */
@Slf4j
@Service
public class ResultQuerier {

    final IndicatorRepoFinder indicatorRepoFinder;

    final JobMapper jobMapper;

    final JobBoConverter jobBoConverter;

    final StorageAccessService storageAccessService;

    public ResultQuerier(
        IndicatorRepoFinder indicatorRepoFinder,
        JobMapper jobMapper, JobBoConverter jobBoConverter,
        StorageAccessService storageAccessService) {

        this.indicatorRepoFinder = indicatorRepoFinder;
        this.jobMapper = jobMapper;
        this.jobBoConverter = jobBoConverter;
        this.storageAccessService = storageAccessService;
    }

    public EvaluationResult resultOfJob(Long jobId){
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(null == jobEntity){
            throw new SWValidationException(ValidSubject.JOB).tip("unknown jobid");
        }
        if(jobEntity.getJobStatus() != JobStatus.FINISHED.getValue()){
            throw new SWValidationException(ValidSubject.JOB).tip("job is not finished yet");
        }
        Job job = jobBoConverter.fromEntity(jobEntity);
        IndicatorRepo indicatorRepo = indicatorRepoFinder.find(job);
        try {
            List<String> results = storageAccessService.list(job.getResultDir()).collect(
                Collectors.toList());
            if(null == results || results.isEmpty()){
                throw new SWValidationException(ValidSubject.JOB).tip("no result found of job");
            }
            Collection<Indicator> indicators = indicatorRepo
                .loadResult(results.get(0));//results.size is expceted to be 1
            return new EvaluationResult("mcResultCollector",indicators);//todo(determined by python)
        } catch (IOException e) {
            throw new SWProcessException(ErrorType.STORAGE).tip("load job ui result failed");
        }

    }

}
