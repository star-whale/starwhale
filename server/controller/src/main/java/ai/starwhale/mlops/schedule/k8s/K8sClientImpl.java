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

import io.kubernetes.client.Exec;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.PatchUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Response;
import org.bouncycastle.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "sw.infra.k8s.enabled", havingValue = "true")
public class K8sClientImpl implements K8sClient {

    private final ApiClient client;
    private final CoreV1Api coreV1Api;
    private final BatchV1Api batchV1Api;
    private final AppsV1Api appsV1Api;

    private final String ns;

    private final SharedInformerFactory informerFactory;

    /**
     * Basic constructor for Kubernetes
     */
    public K8sClientImpl(
            ApiClient client,
            CoreV1Api coreV1Api,
            BatchV1Api batchV1Api,
            AppsV1Api appsV1Api,
            @Value("${sw.infra.k8s.name-space}") String ns,
            SharedInformerFactory informerFactory
    ) {
        this.client = client;
        this.coreV1Api = coreV1Api;
        this.batchV1Api = batchV1Api;
        this.appsV1Api = appsV1Api;
        this.ns = ns;
        this.informerFactory = informerFactory;
    }

    /**
     * deploy apply job to k8s with in this.ns
     *
     * @param job to apply
     * @return submitted job
     */
    @Override
    public V1Job deployJob(V1Job job) throws ApiException {
        return batchV1Api.createNamespacedJob(ns, job, null, null, null, null);
    }

    @Override
    public V1StatefulSet deployStatefulSet(V1StatefulSet ss) throws ApiException {
        return appsV1Api.createNamespacedStatefulSet(ns, ss, null, null, null, null);
    }

    @Override
    public V1Service deployService(V1Service svc) throws ApiException {
        return coreV1Api.createNamespacedService(ns, svc, null, null, null, null);
    }

    @Override
    public V1Secret createSecret(V1Secret secret) throws ApiException {
        return coreV1Api.createNamespacedSecret(ns, secret, null, null, null, null);
    }

    @Override
    public V1Secret replaceSecret(String name, V1Secret secret) throws ApiException {
        return coreV1Api.replaceNamespacedSecret(name, ns, secret, null, null, null, null);
    }

    @Override
    public V1Status deleteSecret(String name) throws ApiException {
        return coreV1Api.deleteNamespacedSecret(name, ns, null, null, null, null, null, null);
    }

    @Override
    public V1Secret getSecret(String name) throws ApiException {
        return coreV1Api.readNamespacedSecret(name, ns, null);
    }

    @Override
    public void deleteJob(String name) throws ApiException {
        batchV1Api.deleteNamespacedJob(name, ns, null, null, 1, false, null, null);
    }

    @Override
    public void deleteStatefulSet(String name) throws ApiException {
        appsV1Api.deleteNamespacedStatefulSet(name, ns, null, null, 1, false, null, null);
    }

    @Override
    public V1Deployment patchDeployment(String deploymentName, V1Patch patch, String patchFormat) throws ApiException {
        return PatchUtils.patch(
                V1Deployment.class,
                () -> appsV1Api.patchNamespacedDeploymentCall(
                        deploymentName,
                        ns,
                        patch,
                        null, null, null, null, null, null),
                patchFormat,
                client);
    }

    @Override
    public V1DeploymentList listDeployment(String labelSelector) throws ApiException {
        return appsV1Api.listNamespacedDeployment(ns, null, null, null, null,
                labelSelector, null, null, null, null, null);
    }


    /**
     * get all jobs with in this.ns
     *
     * @return job list
     */
    @Override
    public V1JobList getJobs(String labelSelector) throws ApiException {
        return batchV1Api.listNamespacedJob(ns, null, null, null, null, labelSelector, null, null, null, 30, null);
    }

    @Override
    public V1StatefulSetList getStatefulSetList(String labelSelector) throws ApiException {
        return appsV1Api.listNamespacedStatefulSet(ns, null, null, null, null,
                labelSelector, null, null, null, 30, null);
    }

    @Override
    public V1PodList getPodList(String labelSelector) throws ApiException {
        return coreV1Api.listNamespacedPod(ns, null, null, null, null, labelSelector, null, null, null, 30, null);
    }

    /**
     * execute command in pod
     *
     * @param podName pod name
     * @param containerName container name, if null, use the first container
     * @param command command to execute
     * @return stdout and stderr, it blocks until command finished
     * @throws IOException when error occurs
     * @throws ApiException when the k8s api call failed
     * @throws InterruptedException when interrupted
     */
    @Override
    public String[] execInPod(String podName, String containerName, String... command)
            throws IOException, ApiException, InterruptedException {
        var exec = new Exec();
        var execCommand = List.of("sh", "-c", String.join(" ", command)).toArray(new String[0]);
        var proc = exec.exec(
                ns,
                podName,
                execCommand,
                containerName,
                false,
                false
        );
        var stdout = proc.getInputStream();
        var stderr = proc.getErrorStream();
        proc.waitFor();
        var out = Strings.fromByteArray(stdout.readAllBytes());
        var err = Strings.fromByteArray(stderr.readAllBytes());
        proc.destroy();
        return new String[]{out, err};
    }

    @Override
    public List<V1Pod> getNotReadyPods(String labelSelector) throws ApiException {
        V1PodList podList = getPodList(labelSelector);
        return podList.getItems()
                .stream()
                .filter(pod -> !isPodReady(pod))
                .collect(Collectors.toList());
    }

    private boolean isPodReady(V1Pod pod) {
        V1PodStatus podStatus = Objects.requireNonNull(pod.getStatus());
        //All conditions statuses are "True"
        return Objects.requireNonNull(podStatus.getConditions())
                .stream()
                .dropWhile(condition -> "True".equalsIgnoreCase(condition.getStatus()))
                .findAny()
                .isEmpty();
    }

    @Override
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

    @Override
    public void watchPod(ResourceEventHandler<V1Pod> eventH, String selector) {
        SharedIndexInformer<V1Pod> podInformer = informerFactory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> coreV1Api.listNamespacedPodCall(ns, null, null, null, null,
                        selector,
                        null, params.resourceVersion, null, params.timeoutSeconds, params.watch,
                        null),
                V1Pod.class,
                V1PodList.class);
        podInformer.addEventHandler(eventH);
        informerFactory.startAllRegisteredInformers();
    }

    @Override
    public void watchStatefulSet(ResourceEventHandler<V1StatefulSet> eventH, String selector) {
        SharedIndexInformer<V1StatefulSet> informer = informerFactory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> appsV1Api.listNamespacedStatefulSetCall(ns, null, null, null, null,
                        selector, null, params.resourceVersion, null, params.timeoutSeconds, params.watch, null),
                V1StatefulSet.class,
                V1StatefulSetList.class);
        informer.addEventHandler(eventH);
        informerFactory.startAllRegisteredInformers();
    }

    @Override
    public void watchEvent(ResourceEventHandler<CoreV1Event> eventH, String selector) {
        SharedIndexInformer<CoreV1Event> informer = informerFactory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> coreV1Api.listNamespacedEventCall(ns, null, null, null, null,
                        selector, null, params.resourceVersion, null, params.timeoutSeconds, params.watch, null),
                CoreV1Event.class,
                CoreV1EventList.class);

        informer.addEventHandler(eventH);
        informerFactory.startAllRegisteredInformers();
    }

    @Override
    public String logOfJob(String selector, List<String> containers) throws ApiException, IOException {
        V1Pod pod = podOfJob(selector);
        return logOfPod(pod, containers);
    }

    @Override
    @NotNull
    public String logOfPod(V1Pod pod, List<String> containers) {
        if (pod == null) {
            return "";
        }

        StringBuilder logBuilder = new StringBuilder();
        containers.forEach(c -> appendLog(pod, logBuilder, c));
        return logBuilder.toString();
    }

    @Override
    @Nullable
    public V1Pod podOfJob(String selector) throws ApiException {
        V1PodList podList = coreV1Api.listNamespacedPod(ns, null, null, null, null, selector, null, null, null, 30,
                null);

        if (podList.getItems().isEmpty()) {
            return null;
        }
        if (podList.getItems().size() > 1) {
            throw new ApiException("to many pods");
        }

        return podList.getItems().get(0);
    }

    private void appendLog(V1Pod pod, StringBuilder logBuilder, String containerName) {
        log.debug("collecting log for container {}", containerName);
        InputStream is = null;
        Response response = null;
        try {
            Call call = readLog(pod.getMetadata().getName(), containerName, false);
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

    @Override
    public Call readLog(String pod, String container, boolean follow) throws IOException, ApiException {
        return coreV1Api.readNamespacedPodLogCall(
                pod,
                ns,
                container,
                follow,
                null,
                null,
                "false",
                false,
                null,
                null,
                null,
                null);
    }

    @Override
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

    @Override
    public V1PodList getPodsByJobName(String job) throws ApiException {
        var selector = K8sClient.toV1LabelSelector(Map.of(K8sJobTemplate.JOB_IDENTITY_LABEL, job));
        return getPodList(selector);
    }

    /**
     * get pod list of the job
     *
     * @param jobId job id
     * @return pod list (empty list if not found, never null)
     */
    @Override
    public List<V1Pod> getPodsByJobNameQuietly(String jobId) {
        try {
            var list = getPodsByJobName(jobId);
            if (list != null) {
                return list.getItems();
            }
        } catch (Exception e) {
            log.warn("failed to get pod of job {}", jobId, e);
        }
        return List.of();
    }

}
