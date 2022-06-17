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

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobManager {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private IDConvertor idConvertor;


    public Long getJobId(String jobUrl) {
        Job job = fromUrl(jobUrl);
        if(job.getId() != null) {
            return job.getId();
        }
        JobEntity jobEntity = jobMapper.findJobByUUID(job.getUuid());
        if(jobEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip(String.format("Unable to find job %s", jobUrl)), HttpStatus.BAD_REQUEST);
        }
        return jobEntity.getId();
    }

    public JobEntity findJob(Job job) {
        JobEntity jobEntity = null;
        if(job.getId() != null) {
            jobEntity = jobMapper.findJobById(job.getId());
        } else if (!StrUtil.isEmpty(job.getUuid())) {
            jobEntity = jobMapper.findJobByUUID(job.getUuid());
        }

        return jobEntity;
    }

    public Job fromUrl(String jobUrl) {
        if(idConvertor.isID(jobUrl)) {
            return Job.builder().id(idConvertor.revert(jobUrl)).build();
        } else {
            return Job.builder().uuid(jobUrl).build();
        }
    }
}
