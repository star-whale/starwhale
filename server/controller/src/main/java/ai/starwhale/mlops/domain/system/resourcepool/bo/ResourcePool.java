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

import ai.starwhale.mlops.schedule.k8s.ResourceOverwriteSpec;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * bo represent agent
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePool {

    public static final String DEFAULT_NAME = "default";
    String name;
    Map<String, String> nodeSelector;
    List<Resource> resources;

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

}
