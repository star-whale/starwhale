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

package ai.starwhale.mlops.schedule.impl.docker;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import com.github.dockerjava.api.model.HostConfig;
import java.util.List;

/**
 * https://docs.docker.com/config/containers/resource_constraints/
 */
public class HostResourceConfigBuilder {

    HostConfig build(List<RuntimeResource> runtimeResources) {
        HostConfig hostConfig = HostConfig.newHostConfig();
        if (null == runtimeResources) {
            return hostConfig;
        }
        runtimeResources.forEach(runtimeResource -> {
            if (ResourceOverwriteSpec.RESOURCE_CPU.equals(runtimeResource.getType())) {
                // docker has no cpu reservation for a container. So, request is not processed
                if (null != runtimeResource.getLimit()) {
                    hostConfig.withCpuCount(runtimeResource.getLimit().longValue());
                }
            }
            if (ResourceOverwriteSpec.RESOURCE_MEMORY.equals(runtimeResource.getType())) {
                // docker has no memory reservation for a container. So, request is not processed
                if (null != runtimeResource.getLimit()) {
                    hostConfig.withMemory(runtimeResource.getLimit().longValue());
                }
            }
        });
        return hostConfig;
    }

}
