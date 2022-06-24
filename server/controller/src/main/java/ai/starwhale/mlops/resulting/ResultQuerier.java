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

package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
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
        try(InputStream inputStream = storageAccessService.get(resultPathOfJob(jobId))){
            return objectMapper.readValue(inputStream,Object.class);
        } catch (IOException e) {
            throw new SWProcessException(ErrorType.STORAGE).tip("load job ui result failed");
        }
    }

    public Map<String, Object> flattenResultOfJob(Long jobId) {
        try(InputStream inputStream = storageAccessService.get(resultPathOfJob(jobId));
            Reader reader = new InputStreamReader(inputStream)){
            JsonFlattener jf = new JsonFlattener(reader);
            return jf.withSeparator('/')
                .ignoreReservedCharacters()
                .flattenAsMap();
        } catch (IOException e) {
            throw new SWProcessException(ErrorType.STORAGE).tip("load job ui result failed");
        }
    }

    public String resultPathOfJob(Long jobId) {
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(null == jobEntity){
            throw new SWValidationException(ValidSubject.JOB).tip("unknown jobid");
        }
        if(jobEntity.getJobStatus() != JobStatus.SUCCESS){
            throw new SWValidationException(ValidSubject.JOB).tip("job is not finished yet");
        }
        try {
            List<String> results = storageAccessService.list(new ResultPath(jobEntity.getResultOutputPath()).resultDir()).collect(
                Collectors.toList());
            if(null == results || results.isEmpty()){
                throw new SWValidationException(ValidSubject.JOB).tip("no result found of job");
            }
            return results.get(0);

        } catch (IOException e) {
            throw new SWProcessException(ErrorType.STORAGE).tip("load job ui result failed");
        }
    }

}
