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

package ai.starwhale.mlops.domain.task.log;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import ai.starwhale.mlops.schedule.k8s.K8sJobTemplate;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskLogK8sCollectorTest {

    StorageAccessService storageService;

    K8sClient k8sClient;

    K8sJobTemplate k8sJobTemplate;

    TaskLogK8sCollector taskLogK8sCollector;

    @BeforeEach
    public void setup() {
        storageService = mock(StorageAccessService.class);
        k8sClient = mock(K8sClient.class);
        k8sJobTemplate = mock(K8sJobTemplate.class);
        taskLogK8sCollector = new TaskLogK8sCollector(storageService, k8sClient, k8sJobTemplate);
    }

    @Test
    public void testNormal() throws IOException, ApiException {
        String log = "this is log";
        when(k8sClient.logOfJob(anyString(), anyList())).thenReturn(log);
        taskLogK8sCollector.collect(Task.builder().id(1L).resultRootPath(new ResultPath("root")).build());
        verify(storageService).put("root/logs/log", log.getBytes(StandardCharsets.UTF_8));

    }


}