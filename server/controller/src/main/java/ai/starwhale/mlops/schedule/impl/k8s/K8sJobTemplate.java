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

package ai.starwhale.mlops.schedule.impl.k8s;

import ai.starwhale.mlops.common.util.FileResourceUtil;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EmptyDirVolumeSource;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Toleration;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(value = "sw.scheduler.impl", havingValue = "k8s")
public class K8sJobTemplate {

    public static final Map<String, String> starwhaleJobLabel = Map.of("owner", "starwhale");

    public static final String JOB_IDENTITY_LABEL = "job-name";

    public static final String PIP_CACHE_VOLUME_NAME = "pip-cache";

    private final String pipCacheHostPath;

    public static final String DEVICE_LABEL_NAME_PREFIX = "device.starwhale.ai-";

    final String evalJobTemplate;

    public K8sJobTemplate(
            @Value("${sw.scheduler.k8s.job.template-path}") String evalJobTemplatePath,
            @Value("${sw.scheduler.k8s.host-path-for-cache}") String pipCacheHostPath
    )
            throws IOException {
        this.evalJobTemplate = FileResourceUtil.getFileContent(evalJobTemplatePath, "template/job.yaml");
        this.pipCacheHostPath = pipCacheHostPath;
    }

    public List<V1Container> getInitContainerTemplates(V1Job job) {
        var containers = job.getSpec().getTemplate().getSpec().getInitContainers();
        return CollectionUtils.isEmpty(containers) ? List.of() : containers;
    }

    public List<V1Container> getContainersTemplates(V1Job job) {
        var containers = job.getSpec().getTemplate().getSpec().getContainers();
        return CollectionUtils.isEmpty(containers) ? List.of() : containers;
    }

    public List<String> getJobContainerNames(V1Job job) {
        return Stream.concat(getInitContainerTemplates(job).stream(), getContainersTemplates(job).stream())
                .map(V1Container::getName)
                .collect(Collectors.toList());
    }

    public V1Job loadJobTemplate() {
        return Yaml.loadAs(evalJobTemplate, V1Job.class);
    }

    public V1Job renderJob(
            V1Job job,
            String jobName,
            String restartPolicy,
            Map<String, ContainerOverwriteSpec> containerSpecMap,
            Map<String, String> nodeSelectors,
            List<Toleration> tolerations,
            Map<String, String> annotations
    ) {
        job.getMetadata().name(jobName);
        V1JobSpec jobSpec = job.getSpec();
        Objects.requireNonNull(jobSpec, "can not get job spec");
        jobSpec.backoffLimit(0);

        V1PodSpec podSpec = jobSpec.getTemplate().getSpec();
        Objects.requireNonNull(podSpec, "can not get pod spec");

        if (tolerations != null && !tolerations.isEmpty()) {
            var originTolerations = podSpec.getTolerations();
            if (originTolerations == null) {
                originTolerations = new ArrayList<>();
            }
            originTolerations.addAll(
                    tolerations.stream()
                            .map(t -> new V1Toleration()
                                    .key(t.getKey())
                                    .operator(t.getOperator())
                                    .value(t.getValue())
                                    .effect(t.getEffect())
                                    .tolerationSeconds(t.getTolerationSeconds())
                            )
                            .collect(Collectors.toList())
            );
            podSpec.tolerations(originTolerations);
        }

        // ensure the pod meta exists
        if (jobSpec.getTemplate().getMetadata() == null) {
            jobSpec.getTemplate().metadata(new V1ObjectMeta());
        }
        updateAnnotations(job.getMetadata(), annotations);
        updateAnnotations(jobSpec.getTemplate().getMetadata(), annotations);
        updateLabels(job, starwhaleJobLabel);
        updateLabels(job, Map.of(JOB_IDENTITY_LABEL, jobName));
        patchPodSpec(restartPolicy, containerSpecMap, nodeSelectors, podSpec);
        patchPipCacheVolume(job.getSpec().getTemplate().getSpec().getVolumes());
        addDeviceInfoLabel(jobSpec.getTemplate(), containerSpecMap);

        return job;
    }

    public void updateLabels(V1Job job, Map<String, String> labels) {
        var originLabels = job.getMetadata().getLabels();
        originLabels = originLabels == null ? new HashMap<>() : originLabels;
        originLabels.putAll(labels);
        job.getMetadata().labels(originLabels);

        var specLabels = job.getSpec().getTemplate().getMetadata().getLabels();
        specLabels = specLabels == null ? new HashMap<>() : specLabels;
        specLabels.putAll(labels);
        job.getSpec().getTemplate().getMetadata().labels(specLabels);
    }

    public void updateAnnotations(V1ObjectMeta meta, Map<String, String> annotations) {
        if (meta == null || annotations == null) {
            return;
        }
        var origin = meta.getAnnotations();
        origin = origin == null ? new HashMap<>() : origin;
        origin.putAll(annotations);
        meta.annotations(origin);
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
            if (!CollectionUtils.isEmpty(containerOverwriteSpec.entrypoint)) {
                c.command(containerOverwriteSpec.entrypoint);
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
     * add device info to the spec, mainly used for ali ask eci-profile
     * <a
     * href="https://www.alibabacloud.com/help/en/elastic-container-instance/latest/configure-elastic-container
     * -instance-profile">more</a>
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
