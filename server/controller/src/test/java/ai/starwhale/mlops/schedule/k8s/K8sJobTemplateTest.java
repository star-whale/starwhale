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

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1PodSpec;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class K8sJobTemplateTest {

    K8sJobTemplate k8sJobTemplate = new K8sJobTemplate("");

    public K8sJobTemplateTest() throws IOException {
    }

    @Test
    public void testInit() {
        List<V1Container> initContainerTemplates = k8sJobTemplate.getInitContainerTemplates();
        List<String> initCnames = initContainerTemplates.stream().map(V1Container::getName)
                .collect(Collectors.toList());
        Assertions.assertIterableEquals(List.of("data-provider", "untar"), initCnames);

        List<V1Container> containerTemplates = k8sJobTemplate.getContainersTemplates();
        List<String> cnames = containerTemplates.stream().map(V1Container::getName)
                .collect(Collectors.toList());
        Assertions.assertIterableEquals(List.of("worker"), cnames);

    }

    @Test
    public void testRender() {
        Map<String, ContainerOverwriteSpec> containerSpecMap = buildContainerSpecMap();
        Map<String, String> nodeSelectors = Map.of("label.pool.bj01", "true");
        V1Job yxz = k8sJobTemplate.renderJob("yxz", containerSpecMap, nodeSelectors);
        V1PodSpec v1PodSpec = yxz.getSpec().getTemplate().getSpec();
        Assertions.assertEquals("true", v1PodSpec.getNodeSelector().get("label.pool.bj01"));
        Assertions.assertEquals(1, v1PodSpec.getNodeSelector().size());

        V1Container workerC = v1PodSpec.getContainers().get(0);
        Assertions.assertIterableEquals(List.of("run"), workerC.getArgs());
        Assertions.assertEquals("image123", workerC.getImage());
        Assertions.assertEquals(new Quantity("200m"), workerC.getResources().getRequests().get("cpu"));
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
                .resourceOverwriteSpec(new ResourceOverwriteSpec(List.of(new RuntimeResource("cpu", 200))))
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

}
