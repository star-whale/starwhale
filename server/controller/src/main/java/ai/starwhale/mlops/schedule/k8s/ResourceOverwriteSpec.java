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
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * map SW resource specification to k8s label selector & resource limit
 * refer to https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
 */
@Slf4j
@Getter
public class ResourceOverwriteSpec {

    V1ResourceRequirements resourceSelector;

    static final String RESOURCE_CPU = "cpu";

    static final String RESOURCE_MEMORY = "memory";

    public static Set<String> SUPPORTED_DEVICES = Set.of(RESOURCE_CPU, "nvidia.com/gpu");

    private Float normalizeNonK8sResources(Float amount) {
        return (float) Math.ceil(amount);
    }

    public ResourceOverwriteSpec(List<RuntimeResource> runtimeResources) {
        Map<String, Quantity> resourceRequiredMap = runtimeResources.stream()
                .collect(convertToMap());
        this.resourceSelector = new V1ResourceRequirements().requests(resourceRequiredMap);
        Map<String, Quantity> resourceLimitMap = runtimeResources.stream()
                .filter(runtimeResource -> !k8sResource(runtimeResource.getType()))
                .collect(convertToMap());
        resourceSelector.limits(resourceLimitMap);
    }

    private Collector<RuntimeResource, ?, Map<String, Quantity>> convertToMap() {
        return Collectors.toMap(RuntimeResource::getType,
                runtimeResource -> new Quantity(
                        k8sResource(runtimeResource.getType()) ? runtimeResource.getNum().toString()
                                : normalizeNonK8sResources(runtimeResource.getNum()).toString()));
    }

    boolean k8sResource(String resource) {
        return RESOURCE_CPU.equals(resource) || RESOURCE_MEMORY.equals(resource);
    }

}
