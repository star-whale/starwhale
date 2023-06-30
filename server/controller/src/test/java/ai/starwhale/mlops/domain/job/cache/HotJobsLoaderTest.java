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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.JobOperator;
import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.init.HotJobsLoader;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HotJobsLoaderTest {

    JobDao jobDao;

    JobOperator jobOperator;

    HotJobsLoader hotJobsLoader;

    @BeforeEach
    public void setup() {
        jobDao = mock(JobDao.class);
        jobOperator = mock(JobOperator.class);
        hotJobsLoader = new HotJobsLoader(
                new JobService(null, null, null,
                    jobDao, jobOperator, null,
                    null, null,
                    null, null, null, null, null,
                    null, null, null));
    }

    @Test
    public void testStartUp() throws Exception {
        Job job1 = Job.builder().id(1L).build();
        Job job2 = Job.builder().id(2L).build();
        when(jobDao.findJobByStatusIn(anyList())).thenReturn(List.of(job1, job2));

        hotJobsLoader.run();
        verify(jobOperator, times(2)).addAndSchedule(any(Job.class), eq(false));
    }

}
