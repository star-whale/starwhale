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

package ai.starwhale.mlops.domain.task;

import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.CANCELED;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.CANCELLING;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.CREATED;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.FAIL;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.PAUSED;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.PREPARING;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.READY;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.RUNNING;
import static ai.starwhale.mlops.domain.job.step.task.status.TaskStatus.SUCCESS;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepService;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.status.StepStatusCalculator;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class StepStatusCalculatorTest {

    StepService stepService = new StepService(
            mock(StepMapper.class), mock(StepConverter.class), mock(TaskService.class));

    @Test
    public void testSuccess() {
        Assertions.assertEquals(StepStatus.SUCCESS, StepStatusCalculator.desiredStepStatus(Set.of(SUCCESS)));
    }

    @Test
    public void testCancelling() {
        StepStatus cancelling = StepStatus.CANCELLING;
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(SUCCESS, CANCELLING)));
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(SUCCESS, CANCELED, PREPARING)));
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(SUCCESS, CANCELLING, CANCELED)));
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(SUCCESS, CANCELLING, RUNNING)));
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(CANCELED, PREPARING, RUNNING, SUCCESS)));
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(CANCELED, READY, PREPARING, RUNNING, SUCCESS)));
        Assertions.assertEquals(cancelling, StepStatusCalculator.desiredStepStatus(
                Set.of(CANCELED, CREATED, PREPARING, RUNNING, SUCCESS)));
    }

    @Test
    public void testCancelled() {
        StepStatus canceled = StepStatus.CANCELED;
        Assertions.assertEquals(canceled, StepStatusCalculator.desiredStepStatus(Set.of(SUCCESS, CANCELED)));
        Assertions.assertEquals(canceled, StepStatusCalculator.desiredStepStatus(Set.of(CREATED, CANCELED)));
    }

    @Test
    public void testPaused() {
        StepStatus paused = StepStatus.PAUSED;
        Assertions.assertEquals(paused, StepStatusCalculator.desiredStepStatus(Set.of(SUCCESS, PAUSED)));
        Assertions.assertEquals(paused, StepStatusCalculator.desiredStepStatus(Set.of(CREATED, PAUSED)));
    }

    @Test
    public void testRunning() {
        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(CREATED, PREPARING)));

        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(CREATED, RUNNING)));

        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(READY, PREPARING)));

        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(READY, SUCCESS)));

        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(READY, RUNNING)));

        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(SUCCESS, RUNNING)));

        Assertions.assertEquals(StepStatus.RUNNING, StepStatusCalculator.desiredStepStatus(
                Set.of(SUCCESS, PREPARING)));
    }

    @Test
    public void testFail() {
        Assertions.assertEquals(StepStatus.FAIL, StepStatusCalculator.desiredStepStatus(
                Set.of(FAIL, SUCCESS)));
        Assertions.assertEquals(StepStatus.FAIL, StepStatusCalculator.desiredStepStatus(
                Set.of(FAIL, RUNNING)));
        Assertions.assertEquals(StepStatus.FAIL, StepStatusCalculator.desiredStepStatus(
                Set.of(FAIL, CANCELLING)));
        Assertions.assertEquals(StepStatus.FAIL, StepStatusCalculator.desiredStepStatus(
                Set.of(FAIL, CREATED, SUCCESS)));
        Assertions.assertEquals(StepStatus.FAIL, StepStatusCalculator.desiredStepStatus(
                Set.of(FAIL, PREPARING, SUCCESS)));

    }

    @Test
    public void testEmpty() {
        Assertions.assertEquals(StepStatus.UNKNOWN, StepStatusCalculator.desiredStepStatus(
                Set.of()));
    }

    @Test
    public void testFirstStep() {
        final Step step1 = Step.builder().id(1L).build();
        final Step step2 = Step.builder().id(2L).build();
        final Step step3 = Step.builder().id(3L).build();
        final Step step4 = Step.builder().id(4L).build();
        final Step step5 = Step.builder().id(5L).build();
        final Step step6 = Step.builder().id(6L).build();
        step3.setNextStep(step2);
        step2.setNextStep(step1);
        step1.setNextStep(step6);
        step6.setNextStep(step4);
        step4.setNextStep(step5);
        Step firsStep = stepService.firsStep(List.of(step1, step2, step3, step4, step5, step6));
        Assertions.assertEquals(step3.getId(), firsStep.getId());
    }
}
