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

import io.kubernetes.client.PodLogs;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.labels.LabelSelector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Response;
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class K8sClient {
    private final ApiClient client;
    private final CoreV1Api coreV1Api;
    private final BatchV1Api batchV1Api;

    private final String ns;

    private final SharedInformerFactory informerFactory;

    private final PodLogs podLogs;

    private final Map<String, String> starwhaleJobLabel = Map.of("owner", "starwhale");

    static final String jobIdentityLabel = "job-name";

    public static final String pipCacheVolumeName = "pip-cache";
    @Value("${sw.infra.k8s.host-path-for-cache}")
    private String pipCacheHostPath;

    /**
     * Basic constructor for Kubernetes
     */
    public K8sClient(@Value("${sw.infra.k8s.name-space}") String ns,ResourceEventHandler<V1Job> eventH,ResourceEventHandler<V1Node> eventHandlerNode)
        throws IOException {
        client =Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        coreV1Api = new CoreV1Api();
        batchV1Api = new BatchV1Api();
        podLogs = new PodLogs();
        this.ns = ns;
        informerFactory = new SharedInformerFactory(client);
        watchJob(eventH);
        watchNode(eventHandlerNode);
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
        batchV1Api.deleteNamespacedJob(id,ns,null,null,1,false,null,null);
//        batchV1Api.deleteNamespacedJobAsync(id,ns,null,null,1,false,null,null,null);
    }

    /**
     * renderJob parses from job yaml template
     *
     * @param template
     * @param coreContainerName
     * @param cmd
     * @return
     */
    public V1Job renderJob(String template, String name, String coreContainerName, String image, List<String> cmd, Map<String, String> coreEnv, Map<String, String> env,V1ResourceRequirements resources) {
        V1Job job = Yaml.loadAs(template, V1Job.class);
        job.getMetadata().name(name);
        HashMap<String, String> labels = new HashMap<>();
        labels.putAll(starwhaleJobLabel);
        labels.put(jobIdentityLabel,name);
        job.getMetadata().labels(labels);
        V1JobSpec jobSpec = job.getSpec();
        Objects.requireNonNull(jobSpec, "can not get job spec");
        V1PodSpec podSpec = jobSpec.getTemplate().getSpec();
        Objects.requireNonNull(podSpec, "can not get pod spec");

        V1Container coreContainer = podSpec.getInitContainers().stream().filter(c -> c.getName().equals(coreContainerName)).findFirst().orElse(null);
        Objects.requireNonNull(coreContainer, "can not get coreContainer by name " + coreContainerName);

        if (!image.isEmpty()) {
            coreContainer.image(image);
        }
        if (!cmd.isEmpty()) {
            coreContainer.args(cmd);
        }
        if(null != resources){
            coreContainer.resources(resources);
        }
        if (!coreEnv.isEmpty()) {
            List<V1EnvVar> ee = new ArrayList<>();
            env.forEach((k, v) -> ee.add(new V1EnvVar().name(k).value(v)));
            coreContainer.env(ee);
        }
        if (!env.isEmpty()) {
            List<V1EnvVar> ee = new ArrayList<>();
            env.forEach((k, v) -> ee.add(new V1EnvVar().name(k).value(v)));
            podSpec.getInitContainers().forEach(c -> {
                if (c.getName().equals(coreContainerName)) {
                    return;
                }
                c.env(ee);
            });
        }

        // replace host path
        List<V1Volume> volumes = job.getSpec().getTemplate().getSpec().getVolumes();
        volumes.stream().filter(v -> v.getName().equals(pipCacheVolumeName))
            .findFirst().ifPresent(volume -> volume.getHostPath().path(pipCacheHostPath));

        return job;
    }

    /**
     * get all jobs with in this.ns
     *
     * @return job list
     */
    public V1JobList get() throws ApiException {
        return batchV1Api.listNamespacedJob(ns, null, null, null, null, toV1LabelSelector(starwhaleJobLabel), null, null, null, 30, null);
    }

    void watchJob(ResourceEventHandler<V1Job> eventH)  {
        SharedIndexInformer<V1Job> jobInformer = informerFactory.sharedIndexInformerFor(
            (CallGeneratorParams params) -> batchV1Api.listNamespacedJobCall(ns, null, null, null, null,
                toV1LabelSelector(starwhaleJobLabel),
                null, params.resourceVersion, null, params.timeoutSeconds, params.watch,
                null),
            V1Job.class,
            V1JobList.class);
        jobInformer.addEventHandler(eventH);
    }

    public String logOfJob(String jobName) throws ApiException, IOException {
        V1PodList podList = coreV1Api.listNamespacedPod(ns, null, null, null, null, toV1LabelSelector(Map.of(
            jobIdentityLabel,jobName)), null, null, null, 30, null);

        if (podList.getItems().isEmpty()) {
            return "";
        }
        if (podList.getItems().size() > 1) {
            throw new ApiException("to many pods");
        }

        V1Pod pod = podList.getItems().get(0);

        StringBuilder  logBuilder = new StringBuilder();
        appendLog(pod, logBuilder,"data-provider");
        appendLog(pod, logBuilder,"untar");
        appendLog(pod, logBuilder,"worker");
        appendLog(pod, logBuilder,"result-uploader");
        return logBuilder.toString();
    }

    private void appendLog(V1Pod pod, StringBuilder logBuilder,String containerName) {
        log.debug("collecting log for container {}",containerName);
        InputStream is=null;
        Response response=null;
        try{
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
        }catch (ApiException e){
            log.warn("k8s api exception",e);
        }catch (IOException e){
            log.error("connection to k8s error",e);
        }finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("closing log fetching error",e);
                }
            }
            if(null != response){
                response.body().close();
            }
        }
        log.debug("log for container {} collected",containerName);
    }

    private String toV1LabelSelector(Map<String, String> labels){
        return LabelSelector.parse(new V1LabelSelector().matchLabels(labels)).toString();
    }

    private void watchNode(ResourceEventHandler<V1Node> eventHandlerNode){
        SharedIndexInformer<V1Node> nodeInformer = informerFactory.sharedIndexInformerFor(
            (CallGeneratorParams params) -> coreV1Api.listNodeCall(null, null, null, null, null,
                null,
                params.resourceVersion, null, params.timeoutSeconds, params.watch,null),
            V1Node.class,
            V1NodeList.class);
        nodeInformer.addEventHandler(eventHandlerNode);
    }

    @EventListener
    public void handleContextRefreshEvent(ApplicationReadyEvent ctxReadyEvt) {
        log.info("spring context ready now");
        informerFactory.startAllRegisteredInformers();
    }
}
