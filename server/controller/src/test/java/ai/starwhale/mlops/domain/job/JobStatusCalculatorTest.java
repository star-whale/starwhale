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

import static ai.starwhale.mlops.domain.job.step.status.StepStatus.CANCELED;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.CANCELLING;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.CREATED;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.FAIL;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.READY;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.RUNNING;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.SUCCESS;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.TO_CANCEL;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.UNKNOWN;

import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusCalculator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class JobStatusCalculatorTest {

    JobStatusCalculator jobStatusCalculator = new JobStatusCalculator();

    @Test
    public void testSuccess() {
        Assertions.assertEquals(JobStatus.SUCCESS, jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS)));
    }

    @Test
    public void testCancelling() {
        JobStatus cancelling = JobStatus.CANCELLING;
        Assertions.assertEquals(cancelling, jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS, CANCELLING)));

        Assertions.assertEquals(cancelling,
                jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS, TO_CANCEL, CANCELLING)));

        Assertions.assertEquals(cancelling, jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS, TO_CANCEL)));

        Assertions.assertEquals(cancelling,
                jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS, TO_CANCEL, CANCELLING, CANCELED)));

        Assertions.assertEquals(cancelling,
                jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS, CANCELLING, CANCELED)));


    }

    @Test
    public void testCancelled() {
        JobStatus canceled = JobStatus.CANCELED;
        Assertions.assertEquals(canceled, jobStatusCalculator.desiredJobStatus(Set.of(SUCCESS, CANCELED)));

        Assertions.assertEquals(JobStatus.UNKNOWN, jobStatusCalculator.desiredJobStatus(Set.of(UNKNOWN, CANCELED)));
    }

    @Test
    public void testRunning() {

        Assertions.assertEquals(JobStatus.RUNNING, jobStatusCalculator.desiredJobStatus(
                Set.of(CREATED, RUNNING)));

        Assertions.assertEquals(JobStatus.RUNNING, jobStatusCalculator.desiredJobStatus(
                Set.of(READY, RUNNING)));

        Assertions.assertEquals(JobStatus.RUNNING, jobStatusCalculator.desiredJobStatus(
                Set.of(READY, SUCCESS)));

        Assertions.assertEquals(JobStatus.RUNNING, jobStatusCalculator.desiredJobStatus(
                Set.of(CREATED, SUCCESS)));

        Assertions.assertEquals(JobStatus.RUNNING, jobStatusCalculator.desiredJobStatus(
                Set.of(READY, RUNNING)));

        Assertions.assertEquals(JobStatus.RUNNING, jobStatusCalculator.desiredJobStatus(
                Set.of(SUCCESS, RUNNING)));

    }

    @Test
    public void testFail() {
        Assertions.assertEquals(JobStatus.FAIL, jobStatusCalculator.desiredJobStatus(
                Set.of(FAIL, SUCCESS)));
        Assertions.assertEquals(JobStatus.FAIL, jobStatusCalculator.desiredJobStatus(
                Set.of(FAIL, RUNNING)));
        Assertions.assertEquals(JobStatus.FAIL, jobStatusCalculator.desiredJobStatus(
                Set.of(FAIL, CANCELLING)));
        Assertions.assertEquals(JobStatus.FAIL, jobStatusCalculator.desiredJobStatus(
                Set.of(FAIL, CREATED, SUCCESS)));

    }

    @Test
    public void testEmpty() {
        Assertions.assertEquals(JobStatus.UNKNOWN, jobStatusCalculator.desiredJobStatus(
                Set.of()));
    }


}
