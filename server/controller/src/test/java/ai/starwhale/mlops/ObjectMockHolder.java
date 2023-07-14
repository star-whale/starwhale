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

import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.agent.AgentConverter;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMockHolder {
    public static JobDao jobDao = mock(JobDao.class);

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
}
