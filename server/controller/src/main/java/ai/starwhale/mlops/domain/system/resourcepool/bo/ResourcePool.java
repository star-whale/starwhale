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

package ai.starwhale.mlops.domain.system.resourcepool.bo;

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * bo represent agent
 */
@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePool {

    public static final String DEFAULT_NAME = "default";
    String name;
    Map<String, String> nodeSelector;
    List<Resource> resources;
    List<Toleration> tolerations;
    // currently used for k8s annotations
    Map<String, String> metadata;

    // private pool only used for internal
    Boolean isPrivate;
    List<Long> visibleUserIds;

    public static ResourcePool defaults() {
        return ResourcePool
                .builder()
                .name(DEFAULT_NAME)
                .nodeSelector(Map.of())
                .resources(List.of(
                        new Resource(ResourceOverwriteSpec.RESOURCE_CPU),
                        new Resource(ResourceOverwriteSpec.RESOURCE_MEMORY)
                ))
                .build();
    }

    public void validateResource(RuntimeResource resource) {
        var type = resource.getType();
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("resource type is empty");
        }
        if (!ResourceOverwriteSpec.SUPPORTED_DEVICES.contains(type)) {
            throw new IllegalArgumentException("unsupported resource type: " + type);
        }
        if (resources == null || resources.isEmpty()) {
            throw new IllegalArgumentException("resource pool is empty");
        }
        var rc = resources.stream().filter(r -> r.getName().equals(type)).findFirst().orElse(null);
        // no rules for the resource
        if (rc == null) {
            throw new IllegalArgumentException("resource pool has no rules for resource type: " + type);
        }
        rc.validate(resource);
    }

    public void validateResources(List<RuntimeResource> runtimeResources) {
        if (runtimeResources == null) {
            return;
        }
        for (var r : runtimeResources) {
            validateResource(r);
        }
    }

    /**
     * patchResource will patch the resource with default value and add resource if not exist
     *
     * @param runtimeResources runtime resource
     * @return patched resource
     */
    public List<RuntimeResource> patchResources(List<RuntimeResource> runtimeResources) {
        if (resources == null || resources.isEmpty()) {
            return runtimeResources;
        }
        var ret = new ArrayList<RuntimeResource>();
        for (var rc : resources) {
            RuntimeResource rr = null;
            if (runtimeResources != null) {
                rr = runtimeResources.stream().filter(i -> i.getType().equals(rc.getName())).findFirst().orElse(null);
            }
            if (rr == null) {
                rr = new RuntimeResource();
                rr.setType(rc.getName());
            } else {
                // clone
                rr = rr.toBuilder().build();
            }
            rc.patch(rr);
            // no request update, ignore
            if (rr.getRequest() == null) {
                log.warn("no request update for {}", rr.getType());
                continue;
            }
            ret.add(rr);
        }
        return ret;
    }

    public List<RuntimeResource> validateAndPatchResource(List<RuntimeResource> runtimeResources) {
        validateResources(runtimeResources);
        return patchResources(runtimeResources);
    }

    public boolean allowUser(Long userId) {
        if (isPrivate != null && isPrivate) {
            if (visibleUserIds == null || visibleUserIds.isEmpty()) {
                return false;
            }
            return visibleUserIds.contains(userId);
        }
        return true;
    }

    public static ResourcePool fromJson(String content) throws IOException {
        return Constants.objectMapper.readValue(content, ResourcePool.class);
    }

    public String toJson() throws IOException {
        return Constants.objectMapper.writeValueAsString(this);
    }
}
