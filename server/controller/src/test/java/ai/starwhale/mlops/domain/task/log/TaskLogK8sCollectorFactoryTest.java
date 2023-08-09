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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.K8sJobTemplate;
import ai.starwhale.mlops.schedule.impl.k8s.log.TaskLogK8sCollectorFactory;
import ai.starwhale.mlops.schedule.log.TaskLogOfflineCollector;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskLogK8sCollectorFactoryTest {

    K8sClient k8sClient;

    K8sJobTemplate k8sJobTemplate;

    TaskLogK8sCollectorFactory taskLogK8sCollector;

    @BeforeEach
    public void setup() {
        k8sClient = mock(K8sClient.class);
        k8sJobTemplate = mock(K8sJobTemplate.class);
        taskLogK8sCollector = new TaskLogK8sCollectorFactory(k8sClient, k8sJobTemplate);
    }

    @Test
    public void testNormal() throws IOException, ApiException {
        String log = "this is log";
        V1Pod v1Pod = new V1Pod().metadata(new V1ObjectMeta().name("x")).status(new V1PodStatus().phase("Running"));
        when(k8sClient.podOfJob(anyString())).thenReturn(v1Pod);
        when(k8sClient.logOfPod(eq(v1Pod), anyList())).thenReturn(log);
        when(k8sClient.getPodsByJobName(any())).thenReturn(new V1PodList().addItemsItem(v1Pod));
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        when(call.execute()).thenReturn(response);
        ResponseBody responseBody = mock(ResponseBody.class);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(k8sClient.readLog("x", "worker", true)).thenReturn(call);
        Assertions.assertInstanceOf(TaskLogOfflineCollector.class, taskLogK8sCollector.offlineCollector(new Task()));
        Assertions.assertInstanceOf(TaskLogStreamingCollector.class,
                taskLogK8sCollector.streamingCollector(Task.builder().id(1L).build()));

    }


}
