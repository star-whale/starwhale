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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


/**
 * test for {@link RunUpdateListenerForDatasetConsumption}
 */
public class RunUpdateListenerForDataConsumptionTest {

    DataLoader dataLoader;
    RunUpdateListenerForDatasetConsumption listener;

    @BeforeEach
    public void setup() {
        dataLoader = mock(DataLoader.class);
        listener = new RunUpdateListenerForDatasetConsumption(dataLoader);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "FAILED,1",
            "FINISHED,0",
            "RUNNING,0",
            "PENDING,0"
    })
    public void testTaskStatusChange(RunStatus status, int resetCount) {
        Run run = Run.builder()
                .id(2L)
                .taskId(1L)
                .status(status)
                .startTime(7L)
                .build();
        listener.onRunUpdate(run);

        verify(dataLoader, times(resetCount)).resetUnProcessed(String.valueOf(run.getTaskId()));
    }

}
