/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
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

