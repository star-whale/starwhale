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
import ai.starwhale.mlops.resulting.pipline.ResultingPPL;
import ai.starwhale.mlops.resulting.pipline.ResultingPPLFinder;
import java.io.IOException;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * coordinate collectors of jobs
 */
@Slf4j
@Service
public class ResultCollectManager {

    final ResultingPPLFinder resultingPPLFinder;

    final JobMapper jobMapper;

    final JobBoConverter jobBoConverter;

    public ResultCollectManager(
        ResultingPPLFinder resultingPPLFinder,
        JobMapper jobMapper, JobBoConverter jobBoConverter) {

        this.resultingPPLFinder = resultingPPLFinder;
        this.jobMapper = jobMapper;
        this.jobBoConverter = jobBoConverter;
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
        ResultingPPL resultingPPL = resultingPPLFinder.findForJob(job);
        try {
            Collection<Indicator> indicators = resultingPPL.getIndicatorRepo()
                .loadUILevel(job.getUuid());
            return new EvaluationResult(resultingPPL.getUniqueName(),indicators);
        } catch (IOException e) {
            throw new SWProcessException(ErrorType.STORAGE).tip("load job ui result failed");
        }

    }

}
