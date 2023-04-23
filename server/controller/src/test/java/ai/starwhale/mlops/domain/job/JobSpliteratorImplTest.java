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

import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.split.JobSpliteratorImpl;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * test for {@link JobSpliteratorImpl}
 */
public class JobSpliteratorImplTest {

    @Test
    public void testJobSpliteratorEvaluation() {
        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockJob = jobMockHolder.mockJob();
        mockJob.setCurrentStep(null);
        mockJob.setSteps(null);
        mockJob.setStatus(JobStatus.CREATED);
        mockJob.getModel().setStepSpecs(List.of(
                        StepSpec.builder()
                            .jobName("default")
                            .name("a")
                            .replicas(1)
                            .resources(List.of())
                            .build(),
                        StepSpec.builder()
                            .jobName("default")
                            .name("b")
                            .replicas(1)
                            .resources(List.of())
                            .needs(List.of("a"))
                            .build(),
                        StepSpec.builder()
                            .jobName("fine_tune")
                            .name("m")
                            .replicas(1)
                            .resources(List.of())
                            .build()
                )
        );
        TaskMapper taskMapper = mock(TaskMapper.class);
        JobDao jobDao = mock(JobDao.class);
        StepMapper stepMapper = mock(StepMapper.class);
        JobSpliteratorImpl jobSpliteratorImpl = new JobSpliteratorImpl(
                new StoragePathCoordinator("/test"), taskMapper, jobDao, stepMapper,
                mock(JobSpecParser.class));

        mockJob.setStepSpec("");
        Assertions.assertThrows(SwValidationException.class, () -> jobSpliteratorImpl.split(mockJob));

        mockJob.setStepSpec("123");
        jobSpliteratorImpl.split(mockJob);

        mockJob.setStatus(JobStatus.RUNNING);
        Assertions.assertThrows(SwValidationException.class, () -> jobSpliteratorImpl.split(mockJob));

    }
}
