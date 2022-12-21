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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HotJobsLoaderTest {

    JobRepo jobRepo;

    JobLoader jobLoader;

    JobStatusMachine jobStatusMachine;

    JobBoConverter jobBoConverter;

    HotJobsLoader hotJobsLoader;

    @BeforeEach
    public void setup() {
        jobRepo = mock(JobRepo.class);
        jobLoader = mock(JobLoader.class);
        jobBoConverter = mock(JobBoConverter.class);
        jobStatusMachine = new JobStatusMachine();
        hotJobsLoader = new HotJobsLoader(jobRepo, jobLoader, jobStatusMachine, jobBoConverter);
    }

    @Test
    public void testStartUp() throws Exception {
        JobEntity job1 = JobEntity.builder().id("1L").build();
        JobEntity job2 = JobEntity.builder().id("2L").build();
        when(jobRepo.findJobByStatusIn(anyList())).thenReturn(List.of(job1, job2));
        Job j = Job.builder().id("1L").build();
        when(jobBoConverter.fromEntity(job1)).thenReturn(j);
        when(jobBoConverter.fromEntity(job2)).thenThrow(new RuntimeException());
        hotJobsLoader.run();
        verify(jobRepo).updateJobStatus("2L", JobStatus.FAIL);
        verify(jobLoader).load(j, false);
    }

}
