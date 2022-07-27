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
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
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
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class K8sClient {
    private ApiClient client;
    private CoreV1Api coreV1Api;
    private final BatchV1Api batchV1Api;

    private final String ns;

    private final SharedInformerFactory informerFactory;

    private final Map<String, String> starwhaleJobLabel = Map.of("owner", "starwhale");

    static final String jobIdentityLabel = "job-name";

    /**
     * Basic constructor for Kubernetes
     */
    public K8sClient(@Value("${sw.infra.k8s.name-space}") String ns,ResourceEventHandler<V1Job> eventH)
        throws IOException {
        client =Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        coreV1Api = new CoreV1Api();
        batchV1Api = new BatchV1Api();
        this.ns = ns;
        informerFactory = new SharedInformerFactory(client);
        watchJob(eventH);
        informerFactory.startAllRegisteredInformers();
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
        batchV1Api.deleteNamespacedJobAsync(id,ns,null,null,1,false,null,null,null);
    }

    /**
     * renderJob parses from job yaml template
     *
     * @param template
     * @param containerName
     * @param cmd
     * @return
     */
    public V1Job renderJob(String template, String name, String containerName, String image, List<String> cmd, Map<String, String> env) {
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

        V1Container container = podSpec.getInitContainers().stream().filter(c -> c.getName().equals(containerName)).findFirst().orElse(null);
        Objects.requireNonNull(container, "can not get container by name " + containerName);

        if (!image.isEmpty()) {
            container.image(image);
        }
        if (!cmd.isEmpty()) {
            container.args(cmd);
        }
        if (!env.isEmpty()) {
            List<V1EnvVar> ee = new ArrayList<>();
            env.forEach((k, v) -> ee.add(new V1EnvVar().name(k).value(v)));
            podSpec.getInitContainers().forEach(c -> {
                if (c.getName().equals(containerName)) {
                    return;
                }
                c.env(ee);
            });
        }

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
        PodLogs logs = new PodLogs();
        StringBuilder  logBuilder = new StringBuilder ();
        InputStream is = logs.streamNamespacedPodLog(ns, pod.getMetadata().getName(), "data-provider");
        logBuilder.append(Strings.fromUTF8ByteArray(is.readAllBytes()));
        is = logs.streamNamespacedPodLog(ns, pod.getMetadata().getName(), "untar");
        logBuilder.append(Strings.fromUTF8ByteArray(is.readAllBytes()));
        is = logs.streamNamespacedPodLog(ns, pod.getMetadata().getName(), "worker");
        logBuilder.append(Strings.fromUTF8ByteArray(is.readAllBytes()));
        is = logs.streamNamespacedPodLog(ns, pod.getMetadata().getName(), "result-uploader");
        logBuilder.append(Strings.fromUTF8ByteArray(is.readAllBytes()));

        return logBuilder.toString();
    }

    private String toV1LabelSelector(Map<String, String> labels){
        return LabelSelector.parse(new V1LabelSelector().matchLabels(labels)).toString();
    }
}
