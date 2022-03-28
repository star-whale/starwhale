/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.gpu;

import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GPUInfo {

    private String id;

    private String name;

    private String driverInfo;

    private String brand;

    private String totalMemory;

    private String usedMemory;

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

