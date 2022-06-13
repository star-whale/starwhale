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

package ai.starwhale.test.domain.task;

import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.StepHelper;

import static ai.starwhale.mlops.domain.task.status.TaskStatus.*;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestStepHelper {

    StepHelper stepHelper = new StepHelper();

    @Test
    public void testSuccess() {
        Assertions.assertEquals(StepStatus.SUCCESS, stepHelper.desiredStepStatus(Set.of(SUCCESS)));
    }

    @Test
    public void testCancelling() {
        StepStatus cancelling = StepStatus.CANCELLING;
        Assertions.assertEquals(cancelling, stepHelper.desiredStepStatus(Set.of(SUCCESS,CANCELLING)));

        Assertions.assertEquals(cancelling, stepHelper.desiredStepStatus(Set.of(SUCCESS,TO_CANCEL,CANCELLING)));

        Assertions.assertEquals(cancelling, stepHelper.desiredStepStatus(Set.of(SUCCESS,TO_CANCEL)));

        Assertions.assertEquals(cancelling, stepHelper.desiredStepStatus(Set.of(SUCCESS,TO_CANCEL,CANCELLING,CANCELED)));

        Assertions.assertEquals(cancelling, stepHelper.desiredStepStatus(Set.of(SUCCESS,CANCELLING,CANCELED)));
        Assertions.assertEquals(cancelling, stepHelper.desiredStepStatus(Set.of(CANCELED, PREPARING, ASSIGNING, RUNNING, SUCCESS)));

    }

    @Test
    public void testCancelled() {
        StepStatus canceled = StepStatus.CANCELED;
        Assertions.assertEquals(canceled, stepHelper.desiredStepStatus(Set.of(SUCCESS,CANCELED)));

    }

    @Test
    public void testRunning() {

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(CREATED,ASSIGNING)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(CREATED,RUNNING)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(READY,ASSIGNING)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(READY,SUCCESS)));

//        Assertions.assertEquals(StepStatus.RUNNING, stepStatusCalculator.desiredStepStatus(
//            Set.of(CREATED,SUCCESS)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(READY,RUNNING)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(SUCCESS,RUNNING)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(SUCCESS,PREPARING)));

        Assertions.assertEquals(StepStatus.RUNNING, stepHelper.desiredStepStatus(
            Set.of(SUCCESS,ASSIGNING)));


    }

    @Test
    public void testFail() {
        Assertions.assertEquals(StepStatus.FAIL, stepHelper.desiredStepStatus(
            Set.of(FAIL,SUCCESS)));
        Assertions.assertEquals(StepStatus.FAIL, stepHelper.desiredStepStatus(
            Set.of(FAIL,RUNNING)));
        Assertions.assertEquals(StepStatus.FAIL, stepHelper.desiredStepStatus(
            Set.of(FAIL,CANCELLING)));
        Assertions.assertEquals(StepStatus.FAIL, stepHelper.desiredStepStatus(
            Set.of(FAIL,CREATED,SUCCESS)));
        Assertions.assertEquals(StepStatus.FAIL, stepHelper.desiredStepStatus(
            Set.of(FAIL,ASSIGNING,SUCCESS)));

    }

    @Test
    public void  testEmpty(){
        Assertions.assertEquals(StepStatus.UNKNOWN, stepHelper.desiredStepStatus(
            Set.of()));
    }


}
