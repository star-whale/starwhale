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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.task.status.watchers.log.CancellableTaskLogK8sCollectorFactory;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CancellableTaskLogK8sCollectorFactoryTest {
    @Test
    public void testMake() throws IOException, ApiException {
        var k8sClient = mock(K8sClient.class);
        when(k8sClient.getPodsByJobName("1"))
                .thenReturn(new V1PodList().items(List.of(new V1Pod().metadata(new V1ObjectMeta().name("1-xx")))));
        var call = mock(Call.class);
        var resp = mock(Response.class);
        var respBody = mock(ResponseBody.class);
        when(respBody.byteStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(resp.body()).thenReturn(respBody);
        when(call.execute()).thenReturn(resp);
        when(k8sClient.readLog(anyString(), anyString())).thenReturn(call);
        var factory = new CancellableTaskLogK8sCollectorFactory(k8sClient);
        var collector = factory.make(1L);
        Assertions.assertNotNull(collector);
    }
}
