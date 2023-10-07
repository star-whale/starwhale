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

package ai.starwhale.mlops.schedule.impl.k8s.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CancellableJobLogK8sCollectorTest {

    K8sClient k8sClient;

    @BeforeEach
    public void setup() {
        k8sClient = mock(K8sClient.class);
    }

    @Test
    public void testInitAndRead() throws IOException, ApiException {
        var runningPodMeta = new V1ObjectMeta().name("running-pod");
        var runningPod = new V1Pod().metadata(runningPodMeta).status(new V1PodStatus().phase("Running"));
        var failedPodMeta = new V1ObjectMeta().name("failed-pod");
        var failedPod = new V1Pod().metadata(failedPodMeta).status(new V1PodStatus().phase("Failed"));
        when(k8sClient.getPodsByJobName("1")).thenReturn(new V1PodList().items(List.of(runningPod, failedPod)));

        var line = "abc";

        var call = mock(Call.class);
        var resp = mock(Response.class);
        var respBody = mock(ResponseBody.class);
        when(respBody.byteStream()).thenReturn(new ByteArrayInputStream(line.getBytes()));
        when(resp.body()).thenReturn(respBody);
        when(call.execute()).thenReturn(resp);

        when(k8sClient.readLog(eq("running-pod"), anyString(), anyBoolean())).thenReturn(call);
        var ins = new RunLogK8sStreamingCollector(k8sClient, "1");

        assertThat(ins.readLine(1L), is(line));
        verify(k8sClient).getPodsByJobName("1");
        verify(call).execute();
    }
}
