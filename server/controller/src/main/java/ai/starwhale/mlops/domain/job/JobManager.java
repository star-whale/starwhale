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

import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobManager implements BundleAccessor, RecoverAccessor {

    private final JobMapper jobMapper;
    private final IdConvertor idConvertor;

    public JobManager(JobMapper jobMapper, IdConvertor idConvertor) {
        this.jobMapper = jobMapper;
        this.idConvertor = idConvertor;
    }

    public Long getJobId(String jobUrl) {
        Job job = fromUrl(jobUrl);
        if (job.getId() != null) {
            return job.getId();
        }
        JobEntity jobEntity = jobMapper.findJobByUuid(job.getUuid());
        if (jobEntity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.JOB)
                    .tip(String.format("Unable to find job %s", jobUrl)), HttpStatus.BAD_REQUEST);
        }
        return jobEntity.getId();
    }

    public JobEntity findJob(Job job) {
        JobEntity jobEntity = null;
        if (job.getId() != null) {
            jobEntity = jobMapper.findJobById(job.getId());
        } else if (!StrUtil.isEmpty(job.getUuid())) {
            jobEntity = jobMapper.findJobByUuid(job.getUuid());
        }

        return jobEntity;
    }

    public Job fromUrl(String jobUrl) {
        if (idConvertor.isId(jobUrl)) {
            return Job.builder().id(idConvertor.revert(jobUrl)).build();
        } else {
            return Job.builder().uuid(jobUrl).build();
        }
    }

    @Override
    public BundleEntity findById(Long id) {
        return jobMapper.findJobById(id);
    }

    @Override
    public BundleEntity findByName(String name, Long projectId) {
        return jobMapper.findJobByUuid(name);
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return jobMapper.findJobById(id);
    }

    @Override
    public List<? extends BundleEntity> listDeletedBundlesByName(String name, Long projectId) {
        List<JobEntity> list = new ArrayList<>();
        JobEntity job = jobMapper.findJobByUuid(name);
        if (job != null) {
            list.add(job);
        }
        return list;
    }

    @Override
    public Boolean recover(Long id) {
        return jobMapper.recoverJob(id) > 0;
    }
}
