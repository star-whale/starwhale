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

package ai.starwhale.mlops;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.cache.HotJobHolderImpl;
import ai.starwhale.mlops.domain.job.status.JobStatusCalculator;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatusMachine;
import ai.starwhale.mlops.domain.job.step.trigger.SimpleStepTrigger;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.agent.AgentConverter;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;

public class ObjectMockHolder {

    public static TaskMapper taskMapper = mock(TaskMapper.class);

    public static JobRepo jobRepo = mock(JobRepo.class);

    public static StepMapper stepMapper = mock(StepMapper.class);

    public static TaskStatusMachine taskStatusMachine() {
        return new TaskStatusMachine();
    }

    public static JobStatusMachine jobStatusMachine() {
        return new JobStatusMachine();
    }

    public static StepStatusMachine stepStatusMachine() {
        return new StepStatusMachine();
    }

    public static StoragePathCoordinator storagePathCoordinator() {
        return new StoragePathCoordinator("/test/sys/starwhale");
    }

    public static ObjectMapper jsonMapper() {
        return new ObjectMapper();
    }

    public static AgentConverter agentConverter() {
        return new AgentConverter(jsonMapper());
    }

    public static TaskBoConverter taskBoConverter() {
        return new TaskBoConverter(agentConverter());
    }

    public static StorageAccessService storageAccessService() {
        return mock(StorageAccessService.class);
    }

    public static SimpleStepTrigger evalPplStepTrigger() {
        StorageAccessService storageAccessService = storageAccessService();
        try {
            when(storageAccessService.list(anyString())).thenReturn(List.of("a", "b", "c").stream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new SimpleStepTrigger(storageAccessService);
    }

    public static HotJobHolderImpl hotJobHolder() {
        return new HotJobHolderImpl();
    }

    public static JobStatusCalculator jobStatusCalculator() {
        return new JobStatusCalculator();
    }


}
