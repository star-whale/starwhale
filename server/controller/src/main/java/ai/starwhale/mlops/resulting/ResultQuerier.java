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

import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
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

    final JobDao jobDao;

    final StorageAccessService storageAccessService;

    final ObjectMapper objectMapper;

    public ResultQuerier(
            JobDao jobDao,
            StorageAccessService storageAccessService,
            ObjectMapper objectMapper) {
        this.jobDao = jobDao;
        this.storageAccessService = storageAccessService;
        this.objectMapper = objectMapper;
    }

    public Object resultOfJob(Long jobId) {
        try (InputStream inputStream = storageAccessService.get(resultPathOfJob(jobId))) {
            return objectMapper.readValue(inputStream, Object.class);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "load job ui result failed", e);
        }
    }

    public Map<String, Object> flattenResultOfJob(Long jobId) {
        try (InputStream inputStream = storageAccessService.get(resultPathOfJob(jobId));
                Reader reader = new InputStreamReader(inputStream)) {
            JsonFlattener jf = new JsonFlattener(reader);
            return jf.withSeparator('/')
                    .ignoreReservedCharacters()
                    .flattenAsMap();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "load job ui result failed", e);
        }
    }

    //    public Map<String, Object> flattenSummaryOfJob(Long jobId) {
    //        try(InputStream inputStream = storageAccessService.get(resultPathOfJob(jobId))){
    //            Map result = objectMapper.readValue(inputStream, Map.class);
    //            Map<String, Object> summary = Map.of(
    //                "kind", result.get("kind"),
    //                "summary", result.get("summary"));
    //            //objectMapper.writeValueAsString(summary)
    //            JsonFlattener jf = new JsonFlattener(objectMapper.writeValueAsString(summary));
    //            return jf.withSeparator('/')
    //                .ignoreReservedCharacters()
    //                .flattenAsMap();
    //        } catch (IOException e) {
    //            throw new SwProcessException(ErrorType.STORAGE).tip("load job ui result failed");
    //        }
    //    }

    public String resultPathOfJob(Long jobId) {
        Job job = jobDao.findJobById(jobId);
        if (null == job) {
            throw new SwValidationException(ValidSubject.JOB, "unknown jobid");
        }
        if (job.getStatus() != JobStatus.SUCCESS) {
            throw new SwValidationException(ValidSubject.JOB, "job is not finished yet");
        }
        try {
            List<String> results = storageAccessService.list(
                    new ResultPath(job.getOutputDir()).resultDir()).collect(
                    Collectors.toList());
            if (results.isEmpty()) {
                throw new SwValidationException(ValidSubject.JOB, "no result found of job");
            }
            return results.get(0);

        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "load job ui result failed", e);
        }
    }

}
