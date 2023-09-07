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

package ai.starwhale.mlops.domain.job.cache;

import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * loading hot jobs
 */
@Service
@Slf4j
public class HotJobsLoader implements RollingUpdateStatusListener {

    final JobDao jobDao;

    final JobLoader jobLoader;

    final JobStatusMachine jobStatusMachine;

    public HotJobsLoader(
            JobDao jobDao,
            JobLoader jobLoader,
            JobStatusMachine jobStatusMachine) {
        this.jobDao = jobDao;
        this.jobLoader = jobLoader;
        this.jobStatusMachine = jobStatusMachine;
    }


    /**
     * load jobs that are not FINISHED/ERROR/CANCELED/CREATED/PAUSED into mem CREATED job has no steps yet, so it will
     * not be loaded here
     *
     * @return tasks of jobs that are not FINISHED/ERROR/CANCELED/CREATED/PAUSED
     */
    private List<Job> hotJobsFromDb() {
        List<JobStatus> hotJobStatuses = Arrays.asList(JobStatus.values())
                .parallelStream()
                .filter(jobStatusMachine::isHot)
                .collect(Collectors.toList());
        return jobDao.findJobByStatusIn(hotJobStatuses);
    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) {

    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.READY_DOWN) {
            hotJobsFromDb().forEach(job -> {
                try {
                    jobLoader.load(job, false);
                } catch (Exception e) {
                    log.error("loading hotting job failed {}", job.getId(), e);
                    jobDao.updateJobStatus(job.getId(), JobStatus.FAIL);
                }
            });
            log.info("hot jobs loaded");
        }
    }
}
