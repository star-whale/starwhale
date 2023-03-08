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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1PodSpec;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class K8sJobTemplateTest {

    K8sJobTemplate k8sJobTemplate = new K8sJobTemplate("", "", "", "/path");

    public K8sJobTemplateTest() throws IOException {
    }

    @Test
    public void testInit() {
        var job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);
        List<V1Container> initContainerTemplates = k8sJobTemplate.getInitContainerTemplates(job);
        List<String> initCnames = initContainerTemplates.stream().map(V1Container::getName)
                .collect(Collectors.toList());
        Assertions.assertIterableEquals(List.of("data-provider"), initCnames);

        List<V1Container> containerTemplates = k8sJobTemplate.getContainersTemplates(job);
        List<String> cnames = containerTemplates.stream().map(V1Container::getName)
                .collect(Collectors.toList());
        Assertions.assertIterableEquals(List.of("worker"), cnames);

    }

    @Test
    public void testRenderJob() {
        Map<String, ContainerOverwriteSpec> containerSpecMap = buildContainerSpecMap();
        Map<String, String> nodeSelectors = Map.of("label.pool.bj01", "true");
        var job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);
        k8sJobTemplate.renderJob(job, "yxz", "OnFailure", 10, containerSpecMap, nodeSelectors);
        Assertions.assertEquals(10, Objects.requireNonNull(job.getSpec()).getBackoffLimit());
        V1PodSpec v1PodSpec = job.getSpec().getTemplate().getSpec();
        Assertions.assertEquals("true", v1PodSpec.getNodeSelector().get("label.pool.bj01"));
        Assertions.assertEquals(1, v1PodSpec.getNodeSelector().size());
        Assertions.assertEquals("OnFailure", v1PodSpec.getRestartPolicy());

        V1Container workerC = v1PodSpec.getContainers().get(0);
        Assertions.assertIterableEquals(List.of("run"), workerC.getArgs());
        Assertions.assertEquals("image123", workerC.getImage());
        Assertions.assertEquals(new Quantity("0.2"), workerC.getResources().getRequests().get("cpu"));
        Assertions.assertEquals(new Quantity("0.2"), workerC.getResources().getLimits().get("cpu"));
        Assertions.assertIterableEquals(
                List.of(new V1EnvVar().name("env1").value("env1value"),
                        new V1EnvVar().name("env2").value("env2value")),
                workerC.getEnv());

        Optional<V1Container> dpCo = v1PodSpec.getInitContainers().stream()
                .filter(c -> c.getName().equals("data-provider")).findAny();
        V1Container dpC = dpCo.get();
        Assertions.assertIterableEquals(
                List.of(new V1EnvVar().name("envx").value("envxvalue"),
                        new V1EnvVar().name("envy").value("envyvalue")),
                dpC.getEnv());
    }

    private Map<String, ContainerOverwriteSpec> buildContainerSpecMap() {
        ContainerOverwriteSpec containerOverwriteSpecWorker = ContainerOverwriteSpec.builder()
                .resourceOverwriteSpec(new ResourceOverwriteSpec(List.of(new RuntimeResource("cpu", 0.2f, 0.2f))))
                .cmds(List.of("run"))
                .name("worker")
                .envs(List.of(new V1EnvVar().name("env1").value("env1value"),
                        new V1EnvVar().name("env2").value("env2value")))
                .image("image123")
                .build();

        ContainerOverwriteSpec containerOverwriteSpecDp = ContainerOverwriteSpec.builder()
                .name("data-provider")
                .envs(List.of(new V1EnvVar().name("envx").value("envxvalue"),
                        new V1EnvVar().name("envy").value("envyvalue")))
                .build();

        return Map.of("worker", containerOverwriteSpecWorker, "data-provider", containerOverwriteSpecDp);
    }

    @Test
    public void testPipCache() throws IOException {
        Map<String, ContainerOverwriteSpec> containerSpecMap = buildContainerSpecMap();
        var job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);
        k8sJobTemplate.renderJob(job, "foo", "OnFailure", 10, containerSpecMap, Map.of());
        var volume = job.getSpec().getTemplate().getSpec().getVolumes().stream()
                .filter(v -> v.getName().equals(K8sJobTemplate.PIP_CACHE_VOLUME_NAME)).findFirst().orElse(null);
        Assertions.assertEquals(volume.getHostPath().getPath(), "/path");

        // empty host path
        var template = new K8sJobTemplate("", "", "", "");
        job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);
        template.renderJob(job, "foo", "OnFailure", 10, containerSpecMap, Map.of());
        volume = job.getSpec().getTemplate().getSpec().getVolumes().stream()
                .filter(v -> v.getName().equals(K8sJobTemplate.PIP_CACHE_VOLUME_NAME)).findFirst().orElse(null);
        Assertions.assertNull(volume.getHostPath());
        Assertions.assertNotNull(volume.getEmptyDir());
    }

    @Test
    public void testDevInfoLabel() {
        Map<String, ContainerOverwriteSpec> containerSpecMap = buildContainerSpecMap();
        var job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);
        k8sJobTemplate.renderJob(job, "foo", "OnFailure", 10, containerSpecMap, Map.of());
        var labels = job.getSpec().getTemplate().getMetadata().getLabels();
        assertThat(labels, hasEntry(K8sJobTemplate.DEVICE_LABEL_NAME_PREFIX + "cpu", "true"));

        var specs = new HashMap<String, ContainerOverwriteSpec>();
        var cpuSpec = new ContainerOverwriteSpec();
        cpuSpec.setResourceOverwriteSpec(new ResourceOverwriteSpec(List.of(new RuntimeResource("cpu", 1f, 1f))));
        specs.put("foo", cpuSpec);
        var gpuSpec = new ContainerOverwriteSpec();
        gpuSpec.setResourceOverwriteSpec(
            new ResourceOverwriteSpec(List.of(new RuntimeResource("nvidia.com/gpu", 1f, 1f)))
        );
        specs.put("bar", gpuSpec);
        specs.put("baz", cpuSpec);

        job = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);
        k8sJobTemplate.renderJob(job, "foo", "OnFailure", 10, specs, Map.of());
        labels = job.getSpec().getTemplate().getMetadata().getLabels();
        assertThat(labels, is(Map.of(K8sJobTemplate.DEVICE_LABEL_NAME_PREFIX + "nvidia.com/gpu", "true",
                K8sJobTemplate.DEVICE_LABEL_NAME_PREFIX + "cpu", "true")));
    }

    @Test
    public void testRenderModelServingOrch() {
        var name = "stateful-set-name";
        var envs = Map.of("foo", "bar");
        var rr = RuntimeResource.builder().type("CPU").request(7f).limit(8f).build();
        var resourceOverwriteSpec = new ResourceOverwriteSpec(List.of(rr));
        var ss = k8sJobTemplate.renderModelServingOrch(name, "img", envs, resourceOverwriteSpec, Map.of("foo", "bar"));
        assertThat(ss.getMetadata().getName(), is(name));
        assertThat(ss.getSpec().getTemplate().getSpec().getNodeSelector(), is(Map.of("foo", "bar")));
        var mainContainer = ss.getSpec().getTemplate().getSpec().getContainers().get(0);
        assertThat(mainContainer.getImage(), is("img"));
        assertThat(mainContainer.getEnv(), is(List.of(new V1EnvVar().name("foo").value("bar"))));
        var containerResources = mainContainer.getResources();
        assertThat(containerResources.getRequests(), is(Map.of("CPU", new Quantity("7.0"))));
        assertThat(containerResources.getLimits(), is(Map.of("CPU", new Quantity("8.0"))));

        // no exception is ok
        k8sJobTemplate.renderModelServingOrch(name, "img", envs, null, null);
    }
}
