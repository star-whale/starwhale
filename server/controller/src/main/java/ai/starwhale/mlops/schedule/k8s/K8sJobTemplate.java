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

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EmptyDirVolumeSource;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1HTTPGetAction;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Probe;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class K8sJobTemplate {

    public static final Map<String, String> starwhaleJobLabel = Map.of("owner", "starwhale");

    public static final String JOB_IDENTITY_LABEL = "job-name";

    public static final String PIP_CACHE_VOLUME_NAME = "pip-cache";

    private final String pipCacheHostPath;

    public static final String DEVICE_LABEL_NAME_PREFIX = "device.starwhale.ai-";
    public static final String LABEL_APP = "app";
    public static final String LABEL_WORKLOAD_TYPE = "starwhale-workload-type";
    public static final String WORKLOAD_TYPE_ONLINE_EVAL = "online-eval";
    public static final int ONLINE_EVAL_PORT_IN_POD = 8080;

    final String evalJobTemplate;
    final String modelServingJobTemplate;
    final V1Job v1Job;

    public K8sJobTemplate(
            @Value("${sw.infra.k8s.job.template-path}") String evalJobTemplatePath,
            @Value("${sw.infra.k8s.model-serving-template-path}") String msPath,
            @Value("${sw.infra.k8s.host-path-for-cache}") String pipCacheHostPath
    )
            throws IOException {
        this.evalJobTemplate = getJobDefaultTemplate(evalJobTemplatePath, "template/job.yaml");
        this.modelServingJobTemplate = getJobDefaultTemplate(msPath, "template/model-serving.yaml");
        v1Job = Yaml.loadAs(evalJobTemplate, V1Job.class);
        this.pipCacheHostPath = pipCacheHostPath;
    }

    public List<V1Container> getInitContainerTemplates() {
        var containers = v1Job.getSpec().getTemplate().getSpec().getInitContainers();
        return CollectionUtils.isEmpty(containers) ? List.of() : containers;
    }

    public List<V1Container> getContainersTemplates() {
        var containers = v1Job.getSpec().getTemplate().getSpec().getContainers();
        return CollectionUtils.isEmpty(containers) ? List.of() : containers;
    }

    public V1Job renderJob(
            String jobName,
            String restartPolicy,
            int backoffLimit,
            Map<String, ContainerOverwriteSpec> containerSpecMap,
            Map<String, String> nodeSelectors
    ) {
        V1Job job = Yaml.loadAs(evalJobTemplate, V1Job.class);
        job.getMetadata().name(jobName);
        HashMap<String, String> labels = new HashMap<>();
        labels.putAll(starwhaleJobLabel);
        labels.put(JOB_IDENTITY_LABEL, jobName);
        job.getMetadata().labels(labels);
        V1JobSpec jobSpec = job.getSpec();
        Objects.requireNonNull(jobSpec, "can not get job spec");
        jobSpec.backoffLimit(backoffLimit);
        V1PodSpec podSpec = jobSpec.getTemplate().getSpec();
        Objects.requireNonNull(podSpec, "can not get pod spec");

        patchPodSpec(restartPolicy, containerSpecMap, nodeSelectors, podSpec);
        patchPipCacheVolume(job.getSpec().getTemplate().getSpec().getVolumes());
        addDeviceInfoLabel(jobSpec.getTemplate(), containerSpecMap);

        return job;
    }

    public V1StatefulSet renderModelServingOrch(
            String name,
            String image,
            Map<String, String> envs,
            ResourceOverwriteSpec resource,
            Map<String, String> nodeSelectors
    ) {
        var ss = Yaml.loadAs(this.modelServingJobTemplate, V1StatefulSet.class);
        Objects.requireNonNull(ss.getMetadata());

        // set name and labels
        ss.getMetadata().name(name);

        var labels = new HashMap<String, String>();
        labels.putAll(Map.of(LABEL_APP, name, LABEL_WORKLOAD_TYPE, WORKLOAD_TYPE_ONLINE_EVAL));
        labels.putAll(starwhaleJobLabel);

        ss.getMetadata().labels(labels);

        var spec = ss.getSpec();
        Objects.requireNonNull(spec);
        spec.getSelector().matchLabels(labels);
        Objects.requireNonNull(spec.getTemplate().getMetadata());
        spec.getTemplate().getMetadata().labels(labels);

        // set container spec
        final String containerName = "worker";
        var cos = new ContainerOverwriteSpec();
        cos.setName(containerName);
        cos.setImage(image);
        cos.setEnvs(envs.entrySet().stream().map(K8sJobTemplate::toEnvVar).collect(Collectors.toList()));
        // add readiness probe
        var readiness = new V1Probe();
        cos.setReadinessProbe(readiness);
        cos.setResourceOverwriteSpec(resource);
        readiness.failureThreshold(3);
        var httpGet = new V1HTTPGetAction();
        readiness.httpGet(httpGet);
        httpGet.path("/");
        httpGet.port(new IntOrString(ONLINE_EVAL_PORT_IN_POD));
        httpGet.scheme("HTTP");

        var containerSpecMap = new HashMap<String, ContainerOverwriteSpec>();
        containerSpecMap.put(containerName, cos);

        var podSpec = spec.getTemplate().getSpec();
        Objects.requireNonNull(podSpec);
        patchPodSpec("Always", containerSpecMap, nodeSelectors, podSpec);
        patchPipCacheVolume(ss.getSpec().getTemplate().getSpec().getVolumes());
        addDeviceInfoLabel(spec.getTemplate(), containerSpecMap);

        return ss;
    }

    private static void patchPodSpec(
            String restartPolicy,
            Map<String, ContainerOverwriteSpec> containerSpecMap,
            Map<String, String> nodeSelectors,
            V1PodSpec podSpec
    ) {
        podSpec.restartPolicy(restartPolicy);
        if (null != nodeSelectors) {
            Map<String, String> templateSelector = podSpec.getNodeSelector();
            if (null != templateSelector) {
                nodeSelectors.putAll(templateSelector);
            }
            podSpec.nodeSelector(nodeSelectors);
        }
        var allContainers = new ArrayList<>(podSpec.getContainers());
        if (!CollectionUtils.isEmpty(podSpec.getInitContainers())) {
            allContainers.addAll(podSpec.getInitContainers());
        }
        allContainers.forEach(c -> {
            ContainerOverwriteSpec containerOverwriteSpec = containerSpecMap.get(c.getName());
            if (null == containerOverwriteSpec) {
                return;
            }
            if (null != containerOverwriteSpec.resourceOverwriteSpec
                    && null != containerOverwriteSpec.resourceOverwriteSpec.getResourceSelector()) {
                c.resources(containerOverwriteSpec.resourceOverwriteSpec.getResourceSelector());
            }
            if (!CollectionUtils.isEmpty(containerOverwriteSpec.cmds)) {
                c.args(containerOverwriteSpec.cmds);
            }
            if (StringUtils.hasText(containerOverwriteSpec.image)) {
                c.image(containerOverwriteSpec.image);
            }
            if (!CollectionUtils.isEmpty(containerOverwriteSpec.envs)) {
                c.env(containerOverwriteSpec.envs);
            }
            if (containerOverwriteSpec.readinessProbe != null) {
                c.readinessProbe(containerOverwriteSpec.readinessProbe);
            }
        });
    }

    private void patchPipCacheVolume(List<V1Volume> volumes) {
        if (volumes == null) {
            return;
        }
        var volume = volumes.stream().filter(v -> v.getName().equals(PIP_CACHE_VOLUME_NAME))
                .findFirst().orElse(null);
        if (volume != null) {
            if (pipCacheHostPath.isEmpty()) {
                // make volume emptyDir
                volume.setHostPath(null);
                volume.emptyDir(new V1EmptyDirVolumeSource());
            } else {
                volume.getHostPath().path(pipCacheHostPath);
            }
        }
    }

    /**
     * getJobDefaultTemplate returns the template content, prefer using the sysFsPath than fallbackRc
     *
     * @param sysFsPath  system filesystem path, return the filesystem file contents when not empty
     * @param fallbackRc resource path when system filesystem path not specified
     * @return template file content
     * @throws IOException when reading template file
     */
    private String getJobDefaultTemplate(String sysFsPath, String fallbackRc) throws IOException {
        if (StringUtils.hasText(sysFsPath)) {
            return Files.readString(Paths.get(sysFsPath));
        }
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(fallbackRc);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * add device info to the spec, mainly used for ali ask eci-profile
     * <a href="https://www.alibabacloud.com/help/en/elastic-container-instance/latest/configure-elastic-container-instance-profile">more</a>
     *
     * @param podTemplateSpec which device info labels will be patched to
     * @param specs           which contains the device info
     */
    private void addDeviceInfoLabel(V1PodTemplateSpec podTemplateSpec, Map<String, ContainerOverwriteSpec> specs) {
        if (podTemplateSpec.getMetadata() == null) {
            podTemplateSpec.metadata(new V1ObjectMeta());
        }
        var meta = podTemplateSpec.getMetadata();
        if (meta.getLabels() == null) {
            meta.labels(new HashMap<>());
        }
        specs.values().forEach(spec -> {
            if (spec.resourceOverwriteSpec == null) {
                return;
            }
            var request = spec.resourceOverwriteSpec.getResourceSelector().getRequests();
            if (request == null) {
                return;
            }
            request.keySet().forEach(rc -> meta.getLabels().put(DEVICE_LABEL_NAME_PREFIX + rc, "true"));
        });
    }

    public static V1EnvVar toEnvVar(Map.Entry<String, String> item) {
        var env = new V1EnvVar();
        env.setName(item.getKey());
        env.setValue(item.getValue());
        return env;
    }
}
