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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class K8sClientTest {

    K8sClient k8sClient;
    ApiClient client;
    CoreV1Api coreV1Api;
    BatchV1Api batchV1Api;
    AppsV1Api appsV1Api;
    SharedInformerFactory informerFactory;
    String nameSpace = "nameSpace";

    @BeforeEach
    public void setUp() throws ApiException {
        client = mock(ApiClient.class);
        coreV1Api = mock(CoreV1Api.class);
        batchV1Api = mock(BatchV1Api.class);
        appsV1Api = mock(AppsV1Api.class);
        informerFactory = mock(SharedInformerFactory.class);
        k8sClient = new K8sClient(client, coreV1Api, batchV1Api, appsV1Api, nameSpace, informerFactory);
    }

    @Test
    public void testDeployJob() throws ApiException {
        V1Job job = new V1Job();
        k8sClient.deployJob(job);
        verify(batchV1Api).createNamespacedJob(eq(nameSpace), eq(job), any(), eq(null), any(), any());
    }

    @Test
    public void testDeleteJob() throws ApiException {
        k8sClient.deleteJob("1");
        verify(batchV1Api).deleteNamespacedJob(eq("1"), eq(nameSpace), eq(null), any(), any(), any(), any(), any());
    }

    @Test
    public void testGetJob() throws ApiException {
        V1JobList t = new V1JobList();
        when(batchV1Api.listNamespacedJob(nameSpace, null, null, null, null, "ls", null, null, null, 30,
                null)).thenReturn(
                t);
        Assertions.assertEquals(t, k8sClient.getJobs("ls"));
    }

    @Test
    public void testLogOfJob() throws ApiException, IOException {

        when(coreV1Api.listNamespacedPod(nameSpace, null, null, null, null, "selector", null, null, null, 30,
                null)).thenReturn(new V1PodList().addItemsItem(new V1Pod().metadata(new V1ObjectMeta().name("pdn"))));

        Call callB = mock(Call.class);
        ResponseBody respbB = mock(ResponseBody.class);
        when(respbB.byteStream()).thenReturn(
                new ByteArrayInputStream("foo".getBytes()), new ByteArrayInputStream("bar".getBytes()));
        Response respB = mock(Response.class);
        when(respB.body()).thenReturn(respbB);
        when(respB.isSuccessful()).thenReturn(true);

        when(callB.execute()).thenReturn(respB);
        when(coreV1Api.readNamespacedPodLogCall(
                eq("pdn"),
                eq(nameSpace),
                anyString(),
                eq(false),
                eq(null),
                eq(null),
                eq("false"),
                eq(false),
                eq(null),
                eq(null),
                eq(null),
                eq(null))).thenReturn(callB);

        String s = k8sClient.logOfJob("selector", List.of("a", "b"));
        Assertions.assertEquals("foobar", s);
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

    @Test
    public void testWatchStatefulSet() {
        SharedIndexInformer<V1StatefulSet> nodeInformer = mock(SharedIndexInformer.class);
        when(informerFactory.sharedIndexInformerFor(any(),
                eq(V1StatefulSet.class),
                eq(V1StatefulSetList.class))).thenReturn(nodeInformer);
        ResourceEventHandler<V1StatefulSet> evh = mock(ResourceEventHandler.class);
        k8sClient.watchStatefulSet(evh, "selector");
        verify(nodeInformer).addEventHandler(evh);
        verify(informerFactory).startAllRegisteredInformers();
    }

    @Test
    public void testGetPodsByJobName() throws ApiException {
        var podMeta = new V1ObjectMeta().name("foo-xxx");
        var pods = new V1PodList().items(List.of(new V1Pod().metadata(podMeta)));
        var label = K8sClient.toV1LabelSelector(Map.of(K8sJobTemplate.JOB_IDENTITY_LABEL, "foo"));
        when(coreV1Api.listNamespacedPod(nameSpace, null, null, null, null, label,
                null, null, null, 30, null)).thenReturn(pods);
        Assertions.assertEquals(k8sClient.getPodsByJobName("foo"), pods);
    }

    @Test
    public void testDeployStatefulSet() throws ApiException {
        var ss = new V1StatefulSet();
        k8sClient.deployStatefulSet(ss);
        verify(appsV1Api).createNamespacedStatefulSet(eq(nameSpace), eq(ss), any(), any(), any(), any());
    }

    @Test
    public void testDeleteStatefulSet() throws ApiException {
        var n = "foo";
        k8sClient.deleteStatefulSet(n);
        verify(appsV1Api).deleteNamespacedStatefulSet(eq(n), eq(nameSpace), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testDeployService() throws ApiException {
        var svc = new V1Service();
        k8sClient.deployService(svc);
        verify(coreV1Api).createNamespacedService(eq(nameSpace), eq(svc), any(), any(), any(), any());
    }
}
