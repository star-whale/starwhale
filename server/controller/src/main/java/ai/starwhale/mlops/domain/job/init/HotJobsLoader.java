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

package ai.starwhale.mlops.domain.job.init;

import ai.starwhale.mlops.domain.job.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * loading hot jobs
 */
@Service
@Order(2)
@Slf4j
public class HotJobsLoader implements CommandLineRunner {

    final JobService jobService;

    public HotJobsLoader(JobService jobService) {
        this.jobService = jobService;
    }


    @Override
    public void run(String... args) throws Exception {
        jobService.reloadHotJobs();
    }
}
