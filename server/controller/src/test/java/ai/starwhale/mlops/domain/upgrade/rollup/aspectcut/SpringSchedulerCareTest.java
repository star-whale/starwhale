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

package ai.starwhale.mlops.domain.upgrade.rollup.aspectcut;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

/**
 * a test for SpringSchedulerCare
 */
public class SpringSchedulerCareTest {

    @Test
    public void testNewInstanceReadyUp() throws Throwable {
        SpringSchedulerCare springSchedulerCare = new SpringSchedulerCare();
        ProceedingJoinPoint mockPoint = mock(ProceedingJoinPoint.class);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(0)).proceed();
        springSchedulerCare.onNewInstanceStatus(ServerInstanceStatus.READY_UP);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(0)).proceed();
    }

    @Test
    public void testNewInstanceDown() throws Throwable {
        SpringSchedulerCare springSchedulerCare = new SpringSchedulerCare();
        ProceedingJoinPoint mockPoint = mock(ProceedingJoinPoint.class);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(0)).proceed();
        springSchedulerCare.onNewInstanceStatus(ServerInstanceStatus.DOWN);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(1)).proceed();
    }

    @Test
    public void testOldInstance() throws Throwable {
        SpringSchedulerCare springSchedulerCare = new SpringSchedulerCare();
        ProceedingJoinPoint mockPoint = mock(ProceedingJoinPoint.class);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(0)).proceed();
        springSchedulerCare.onOldInstanceStatus(ServerInstanceStatus.DOWN);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(1)).proceed();
        reset(mockPoint);
        springSchedulerCare.onNewInstanceStatus(ServerInstanceStatus.READY_UP);
        springSchedulerCare.scheduleCare(mockPoint);
        verify(mockPoint, times(0)).proceed();
    }

}
