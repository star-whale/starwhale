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

package ai.starwhale.mlops.domain.run;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.run.RunVo;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunServiceTest {

    private RunDao runDao;

    private RunService runService;

    @BeforeEach
    void init() {
        runDao = mock(RunDao.class);
        runService = new RunService(runDao);
        when(runDao.findByTaskId(any())).thenReturn(List.of(
                Run.builder()
                        .id(1L)
                        .status(RunStatus.RUNNING)
                        .logDir("anu")
                        .runSpec(RunSpec.builder().build())
                        .ip("ip")
                        .taskId(1L)
                        .startTime(122334455L)
                        .finishTime(122334456L)
                        .failedReason("fr")
                        .build()
        ));
    }

    @Test
    void runOfTask() {
        List<RunVo> runVos = runService.runOfTask(1L);
        Assertions.assertEquals(1, runVos.size());
        Assertions.assertEquals(1L, runVos.get(0).getId());
        Assertions.assertEquals(RunStatus.RUNNING, runVos.get(0).getStatus());
        Assertions.assertEquals("ip", runVos.get(0).getIp());
        Assertions.assertEquals(122334455L, runVos.get(0).getStartTime());
        Assertions.assertEquals(122334456L, runVos.get(0).getFinishTime());
        Assertions.assertEquals("fr", runVos.get(0).getFailedReason());
    }
}