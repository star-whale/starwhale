/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * coordinate collectors of jobs
 */
@Slf4j
@Service
public class ResultQuerier {

    final JobMapper jobMapper;

    final StorageAccessService storageAccessService;

    final ObjectMapper objectMapper;

    public ResultQuerier(
        JobMapper jobMapper,
        StorageAccessService storageAccessService,
        ObjectMapper objectMapper) {
        this.jobMapper = jobMapper;
        this.storageAccessService = storageAccessService;
        this.objectMapper = objectMapper;
    }

    public Object resultOfJob(Long jobId){
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(null == jobEntity){
            throw new SWValidationException(ValidSubject.JOB).tip("unknown jobid");
        }
        if(jobEntity.getJobStatus() != JobStatus.SUCCESS){
            throw new SWValidationException(ValidSubject.JOB).tip("job is not finished yet");
        }
        try {
            List<String> results = storageAccessService.list(jobEntity.getResultOutputPath()).collect(
                Collectors.toList());
            if(null == results || results.isEmpty()){
                throw new SWValidationException(ValidSubject.JOB).tip("no result found of job");
            }
            try(InputStream inputStream = storageAccessService.get(results.get(0))){
                return objectMapper.readValue(inputStream,Object.class);
            }

        } catch (IOException e) {
            throw new SWProcessException(ErrorType.STORAGE).tip("load job ui result failed");
        }

    }

}
