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

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * map SW resource specification to k8s label selector & resource limit
 * refer to https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
 */
@Slf4j
@Getter
@EqualsAndHashCode
public class ResourceOverwriteSpec {

    V1ResourceRequirements resourceSelector;

    public static final String RESOURCE_CPU = "cpu";

    public static final String RESOURCE_MEMORY = "memory";

    static final String RESOURCE_GPU = "nvidia.com/gpu";

    public static Set<String> K8S_BUILTIN_RESOURCES = Set.of(RESOURCE_CPU, RESOURCE_MEMORY);

    public static Set<String> SUPPORTED_DEVICES = Set.of(RESOURCE_CPU, RESOURCE_GPU, RESOURCE_MEMORY);

    private Float normalizeNonK8sResources(Float amount) {
        return (float) Math.ceil(amount);
    }

    public ResourceOverwriteSpec(List<RuntimeResource> runtimeResources) {
        Map<String, Quantity> resourceRequestMap = new HashMap<>();
        Map<String, Quantity> resourceLimitMap = new HashMap<>();

        runtimeResources.forEach(resource -> {
            var resourceType = resource.getType();
            var request = resource.getRequest();
            var limit = resource.getLimit();
            if (request != null) {
                resourceRequestMap.put(resourceType, convertToQuantity(resourceType, request));
            }
            if (limit != null) {
                resourceLimitMap.put(resourceType, convertToQuantity(resourceType, limit));
            } else if (request != null && !k8sResource(resourceType)) {
                // use request as limit for non-k8s resources if limit is not specified
                resourceLimitMap.put(resourceType, convertToQuantity(resourceType, request));
            }
        });

        this.resourceSelector = new V1ResourceRequirements()
                .requests(resourceRequestMap)
                .limits(resourceLimitMap);
    }

    private Quantity convertToQuantity(String type, Float num) {
        return new Quantity(
            k8sResource(type) ? num.toString()
                : normalizeNonK8sResources(num).toString());
    }

    boolean k8sResource(String resource) {
        return K8S_BUILTIN_RESOURCES.contains(resource);
    }

}
