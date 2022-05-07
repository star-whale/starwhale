/**
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

package ai.starwhale.mlops.agent.node.gpu;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class GPUInfo {

    /**
     * uuid, Eg: GPU-2edf82f7-d4ce-60ef-e81c-850f5fc277a0
     */
    private String id;

    /**
     * Eg: GeForce MX330, GeForce RTX 2070, Tesla T4 etc.
     */
    private String name;

    /**
     * Eg: "driver version: 451.80,cuda version: 11.0"
     */
    private String driverInfo;

    /**
     * Eg: GeForce
     */
    private String brand;

    /**
     * Eg: 2048 MiB
     */
    private String totalMemory;

    /**
     * Eg: 64MiB
     */
    private String usedMemory;

    /**
     * Eg: 1984MiB
     */
    private String freeMemory;

    /**
     * usage, max=100
     */
    private int usageRate;
    /**
     * the processors which using current gpu
     */
    private List<ProcessInfo> processInfos;
    @Data
    @Builder
    public static class ProcessInfo {

        private String pid;
        private String name;
        private String usedMemory;

    }
}

