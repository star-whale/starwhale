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

import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobManager implements BundleAccessor, RecoverAccessor {

    private final JobRepo jobRepo;

    public JobManager(JobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }

    public JobEntity findJob(String id) {
        return jobRepo.findJobById(id);
    }

    @Override
    public BundleEntity findById(Object id) {
        return jobRepo.findJobById((String) id);
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return jobRepo.findJobById(name);
    }

    @Override
    public BundleEntity findDeletedBundleById(Object id) {
        return jobRepo.findJobById((String) id);
    }


    @Override
    public Boolean recover(Object id) {
        return jobRepo.recoverJob((String) id) > 0;
    }
}
