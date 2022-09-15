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

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.labels.LabelSelector;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Response;
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class K8sClient {

    private final ApiClient client;
    private final CoreV1Api coreV1Api;
    private final BatchV1Api batchV1Api;

    private final String ns;

    private final SharedInformerFactory informerFactory;

    /**
     * Basic constructor for Kubernetes
     */
    public K8sClient(ApiClient client, CoreV1Api coreV1Api, BatchV1Api batchV1Api,
            @Value("${sw.infra.k8s.name-space}") String ns, SharedInformerFactory informerFactory) {
        this.client = client;
        this.coreV1Api = coreV1Api;
        this.batchV1Api = batchV1Api;
        this.ns = ns;
        this.informerFactory = informerFactory;
    }

    /**
     * deploy apply job to k8s with in this.ns
     *
     * @param job to apply
     * @return submitted job
     */
    public V1Job deploy(V1Job job) throws ApiException {
        return batchV1Api.createNamespacedJob(ns, job, null, null, null, null);
    }

    public void deleteJob(String id) throws ApiException {
        batchV1Api.deleteNamespacedJob(id, ns, null, null, 1, false, null, null);
    }

    /**
     * get all jobs with in this.ns
     *
     * @return job list
     */
    public V1JobList get(String labelSelector) throws ApiException {
        return batchV1Api.listNamespacedJob(ns, null, null, null, null, labelSelector, null, null, null, 30, null);
    }

    public void watchJob(ResourceEventHandler<V1Job> eventH, String selector) {
        SharedIndexInformer<V1Job> jobInformer = informerFactory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> batchV1Api.listNamespacedJobCall(ns, null, null, null, null,
                        selector,
                        null, params.resourceVersion, null, params.timeoutSeconds, params.watch,
                        null),
                V1Job.class,
                V1JobList.class);
        jobInformer.addEventHandler(eventH);
        informerFactory.startAllRegisteredInformers();
    }

    public String logOfJob(String selector, List<String> containers) throws ApiException, IOException {
        V1PodList podList = coreV1Api.listNamespacedPod(ns, null, null, null, null, selector, null, null, null, 30,
                null);

        if (podList.getItems().isEmpty()) {
            return "";
        }
        if (podList.getItems().size() > 1) {
            throw new ApiException("to many pods");
        }

        V1Pod pod = podList.getItems().get(0);

        StringBuilder logBuilder = new StringBuilder();
        containers.forEach(c -> appendLog(pod, logBuilder, c));
        return logBuilder.toString();
    }

    private void appendLog(V1Pod pod, StringBuilder logBuilder, String containerName) {
        log.debug("collecting log for container {}", containerName);
        InputStream is = null;
        Response response = null;
        try {
            Call call =
                    coreV1Api.readNamespacedPodLogCall(
                            pod.getMetadata().getName(),
                            ns,
                            containerName,
                            true,
                            null,
                            null,
                            "false",
                            false,
                            null,
                            null,
                            null,
                            null);
            response = call.execute();
            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), "Logs request failed: " + response.code());
            }
            is = response.body().byteStream();
            logBuilder.append(Strings.fromUTF8ByteArray(is.readAllBytes()));
        } catch (ApiException e) {
            log.warn("k8s api exception", e);
        } catch (IOException e) {
            log.error("connection to k8s error", e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("closing log fetching error", e);
                }
            }
            if (null != response) {
                response.body().close();
            }
        }
        log.debug("log for container {} collected", containerName);
    }

    public void watchNode(ResourceEventHandler<V1Node> eventHandlerNode) {
        SharedIndexInformer<V1Node> nodeInformer = informerFactory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> coreV1Api.listNodeCall(null, null, null, null, null,
                        null,
                        params.resourceVersion, null, params.timeoutSeconds, params.watch, null),
                V1Node.class,
                V1NodeList.class);
        nodeInformer.addEventHandler(eventHandlerNode);
        informerFactory.startAllRegisteredInformers();
    }

    public static String toV1LabelSelector(Map<String, String> labels) {
        return LabelSelector.parse(new V1LabelSelector().matchLabels(labels)).toString();
    }


}
