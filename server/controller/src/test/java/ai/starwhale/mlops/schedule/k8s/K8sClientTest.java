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

package ai.starwhale.mlops.schedule.k8s;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class K8sClientTest {

    K8sClient k8sClient;
    ApiClient client;
    CoreV1Api coreV1Api;
    BatchV1Api batchV1Api;
    SharedInformerFactory informerFactory;
    String nameSpace = "nameSpace";

    @BeforeEach
    public void setUp() {
        client = mock(ApiClient.class);
        coreV1Api = mock(CoreV1Api.class);
        batchV1Api = mock(BatchV1Api.class);
        informerFactory = mock(SharedInformerFactory.class);
        k8sClient = new K8sClient(client, coreV1Api, batchV1Api, nameSpace, informerFactory);
    }

    @Test
    public void testDeploy() throws ApiException {
        V1Job job = new V1Job();
        k8sClient.deploy(job);
        verify(batchV1Api).createNamespacedJob(eq(nameSpace), eq(job), any(), eq(null), any(), any());
    }

    @Test
    public void testDeleteJob() throws ApiException {
        k8sClient.deleteJob("1");
        verify(batchV1Api).deleteNamespacedJob(eq("1"), eq(nameSpace), eq(null), any(), any(), any(), any(), any());
    }

    @Test
    public void testGet() throws ApiException {
        V1JobList t = new V1JobList();
        when(batchV1Api.listNamespacedJob(nameSpace, null, null, null, null, "ls", null, null, null, 30,
                null)).thenReturn(
                t);
        Assertions.assertEquals(t, k8sClient.get("ls"));
    }

    @Test
    public void testLogOfJob() throws ApiException, IOException {

        when(coreV1Api.listNamespacedPod(nameSpace, null, null, null, null, "selector", null, null, null, 30,
                null)).thenReturn(new V1PodList().addItemsItem(new V1Pod().metadata(new V1ObjectMeta().name("pdn"))));

        Call callA = mock(Call.class);
        ResponseBody respb = mock(ResponseBody.class);
        BufferedSource bf = mock(BufferedSource.class);
        when(bf.inputStream()).thenReturn(new ByteArrayInputStream("logs_logs".getBytes(StandardCharsets.UTF_8)));
        when(respb.source()).thenReturn(bf);
        Response resp = new Response(new Request(new HttpUrl("", "", "", "", 89, List.of(), null, null, ""), "post",
                new Headers.Builder().build(), null,
                Map.of()), Protocol.HTTP_1_0, "null", 200, null, new Headers.Builder().build(), respb, null, null, null,
                1L, 1L, null);

        when(callA.execute()).thenReturn(resp);
        when(coreV1Api.readNamespacedPodLogCall(
                eq("pdn"),
                eq(nameSpace),
                eq("a"),
                eq(true),
                eq(null),
                eq(null),
                eq("false"),
                eq(false),
                eq(null),
                eq(null),
                eq(null),
                eq(null))).thenReturn(callA);

        Call callB = mock(Call.class);
        ResponseBody respbB = mock(ResponseBody.class);
        BufferedSource bfB = mock(BufferedSource.class);
        when(bfB.inputStream()).thenReturn(new ByteArrayInputStream("logs_logs".getBytes(StandardCharsets.UTF_8)));
        when(respbB.source()).thenReturn(bfB);
        Response respB = new Response(new Request(new HttpUrl("", "", "", "", 89, List.of(), null, null, ""), "post",
                new Headers.Builder().build(), null,
                Map.of()), Protocol.HTTP_1_0, "null", 200, null, new Headers.Builder().build(), respbB, null, null,
                null,
                1L, 1L, null);

        when(callB.execute()).thenReturn(respB);
        when(coreV1Api.readNamespacedPodLogCall(
                eq("pdn"),
                eq(nameSpace),
                eq("b"),
                eq(true),
                eq(null),
                eq(null),
                eq("false"),
                eq(false),
                eq(null),
                eq(null),
                eq(null),
                eq(null))).thenReturn(callB);

        String s = k8sClient.logOfJob("selector", List.of("a", "b"));
        Assertions.assertEquals("logs_logslogs_logs", s);
    }

    @Test
    public void testWatchJob() {
        SharedIndexInformer<V1Job> jobInformer = mock(SharedIndexInformer.class);
        when(informerFactory.sharedIndexInformerFor(any(),
                eq(V1Job.class),
                eq(V1JobList.class))).thenReturn(jobInformer);
        ResourceEventHandler<V1Job> evh = mock(ResourceEventHandler.class);
        k8sClient.watchJob(evh, "selector");
        verify(jobInformer).addEventHandler(evh);
        verify(informerFactory).startAllRegisteredInformers();

    }

    @Test
    public void testWatchNode() {
        SharedIndexInformer<V1Node> nodeInformer = mock(SharedIndexInformer.class);
        when(informerFactory.sharedIndexInformerFor(any(),
                eq(V1Node.class),
                eq(V1NodeList.class))).thenReturn(nodeInformer);
        ResourceEventHandler<V1Node> evh = mock(ResourceEventHandler.class);
        k8sClient.watchNode(evh);
        verify(nodeInformer).addEventHandler(evh);
        verify(informerFactory).startAllRegisteredInformers();
    }


}
