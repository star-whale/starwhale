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

package ai.starwhale.mlops.schedule.reporting.listener.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.log.RunLogSaver;
import ai.starwhale.mlops.schedule.reporting.listener.impl.RunUpdateListenerForGc.RunToBeDeleted;
import java.time.Instant;
import java.util.concurrent.DelayQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class RunUpdateListenerForGcTest {

    RunLogSaver runLogSaver;
    DelayQueue<RunToBeDeleted> runToDeletes;

    RunExecutor runExecutor;

    RunUpdateListenerForGc runUpdateListenerForGc;

    @BeforeEach
    public void setUp() {
        runLogSaver = mock(RunLogSaver.class);
        runExecutor = mock(RunExecutor.class);
        runToDeletes = new DelayQueue<>();
    }

    @Test
    public void testDelayStopSchedule() throws InterruptedException {
        runUpdateListenerForGc = new RunUpdateListenerForGc(runLogSaver, 1L, runExecutor);
        Run run = Run.builder()
                .id(1L)
                .build();
        long current = System.currentTimeMillis() - 1000 * 60L + 1000; // +1s prevent immediately deletion

        Instant instant = Instant.ofEpochMilli(current);
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now).thenReturn(instant);
            run.setStatus(RunStatus.FINISHED);
            runUpdateListenerForGc.onRunUpdate(run);
            verify(runExecutor, times(0)).remove(run);
            runUpdateListenerForGc.processTaskDeletion();
            verify(runExecutor, times(0)).remove(run);
            Thread.sleep(2000);
            runUpdateListenerForGc.processTaskDeletion();
            verify(runExecutor, times(1)).remove(run);
        }
    }

    @Test
    public void testDelayStopScheduleWithNegativeDelay() throws InterruptedException {
        runUpdateListenerForGc = new RunUpdateListenerForGc(runLogSaver, -1L, runExecutor);
        Run run = Run.builder()
                .id(1L)
                .build();
        long current = System.currentTimeMillis() - 1000 * 60L + 1000;

        Instant instant = Instant.ofEpochMilli(current);
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now).thenReturn(instant);
            run.setStatus(RunStatus.FAILED);
            runUpdateListenerForGc.onRunUpdate(run);
            verify(runExecutor, times(1)).remove(run);
            runUpdateListenerForGc.processTaskDeletion();
            verify(runExecutor, times(1)).remove(run);
            Thread.sleep(2000);
            runUpdateListenerForGc.processTaskDeletion();
            verify(runExecutor, times(1)).remove(run);
        }
    }
}
