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
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobManager implements BundleAccessor, RecoverAccessor {

    private final JobRepo jobRepo;
    private final IdConverter idConvertor;

    public JobManager(JobRepo jobRepo, IdConverter idConvertor) {
        this.jobRepo = jobRepo;
        this.idConvertor = idConvertor;
    }

    public Long getJobId(String jobUrl) {
        if (idConvertor.isId(jobUrl)) {
            return idConvertor.revert(jobUrl);
        }
        JobEntity jobEntity = jobRepo.findJobByUuid(jobUrl);
        if (jobEntity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(SwValidationException.ValidSubject.JOB,
                        String.format("Unable to find job %s", jobUrl)),
                    HttpStatus.BAD_REQUEST);
        }
        return jobEntity.getId();
    }

    public Boolean updateJobComment(String jobUrl, String comment) {
        int res;
        if (idConvertor.isId(jobUrl)) {
            res = jobRepo.updateJobComment(idConvertor.revert(jobUrl), comment);
        } else {
            res = jobRepo.updateJobCommentByUuid(jobUrl, comment);
        }
        return res > 0;
    }

    public JobEntity findJob(String jobUrl) {
        if (idConvertor.isId(jobUrl)) {
            return jobRepo.findJobById(idConvertor.revert(jobUrl));
        } else {
            return jobRepo.findJobByUuid(jobUrl);
        }
    }

    @Override
    public BundleEntity findById(Long id) {
        return jobRepo.findJobById(id);
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return jobRepo.findJobByUuid(name);
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return jobRepo.findJobById(id);
    }


    @Override
    public Boolean recover(Long id) {
        return jobRepo.recoverJob(id) > 0;
    }
}
