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

package ai.starwhale.mlops.schedule.reporting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.run.RunDao;
import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.reporting.listener.RunUpdateListener;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RunReportReceiverImplTest {
    RunReportReceiverImpl runReportReceiver;
    RunMapper runMapper;

    RunDao runDao;

    List<RunUpdateListener> runUpdateListeners;

    @BeforeEach
    public void setup() {
        runMapper = mock(RunMapper.class);
        runDao = mock(RunDao.class);
        RunUpdateListener listener = mock(RunUpdateListener.class);
        runUpdateListeners = List.of(listener);
        JobBoConverter jobBoConverter = mock(JobBoConverter.class);
        Job job = mock(Job.class);
        when(job.getTask(any())).thenReturn(new Task());
        when(jobBoConverter.fromTaskId(any())).thenReturn(job);
        runReportReceiver = new RunReportReceiverImpl(
                runMapper,
                runDao,
                runUpdateListeners
        );
    }

    @Test
    public void testStatusChanged() {
        var reportedRun = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .build();
        var runEntity = RunEntity.builder()
                .id(1L)
                .status(RunStatus.FINISHED)
                .build();
        when(runMapper.getForUpdate(1L)).thenReturn(runEntity);
        runReportReceiver.receive(reportedRun);
        verify(runMapper).update(runEntity);
        verify(runUpdateListeners.get(0)).onRunUpdate(any());
    }

    @Test
    public void testStatusUnchanged() {
        var reportedRun = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .build();
        var runEntity = RunEntity.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .build();
        when(runMapper.getForUpdate(1L)).thenReturn(runEntity);
        runReportReceiver.receive(reportedRun);
        verify(runMapper, never()).update(runEntity);
        verify(runUpdateListeners.get(0), never()).onRunUpdate(any());
    }

    @Test
    public void testFieldChanged() {
        var reportedRun = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .startTimeMillis(System.currentTimeMillis())
                .stopTimeMillis(System.currentTimeMillis())
                .ip("a")
                .build();
        var runEntity = RunEntity.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .ip("b")
                .build();
        when(runMapper.getForUpdate(1L)).thenReturn(runEntity);
        runReportReceiver.receive(reportedRun);
        verify(runMapper).update(runEntity);
    }

    @Test
    public void testFieldUnchanged() {
        var reportedRun = ReportedRun.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .ip("a")
                .build();
        var runEntity = RunEntity.builder()
                .id(1L)
                .status(RunStatus.RUNNING)
                .ip("a")
                .build();
        when(runMapper.getForUpdate(1L)).thenReturn(runEntity);
        runReportReceiver.receive(reportedRun);
        verify(runMapper, never()).update(runEntity);
    }
}
