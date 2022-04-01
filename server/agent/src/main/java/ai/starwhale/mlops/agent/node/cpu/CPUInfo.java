/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.cpu;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CPUInfo {

    /**
     * CPU核心数
     */
    private Integer cpuNum;

    /**
     * CPU total usage, Eg:9.01
     */
    private double total;

    /**
     * CPU sys usage,Eg:0.01
     */
    private double sys;

    /**
     * CPU usr usage,Eg:0.06
     */
    private double user;

    /**
     * CPU wait
     */
    private double wait;

    /**
     * CPU idel, Eg:90
     */
    private double free;

    /**
     * CPU model brand,Eg:
     *  Intel(R) Xeon(R) CPU E5-2680 v4 @ 2.40GHz
     *  2 physical CPU package(s)
     *  28 physical CPU core(s)
     *  56 logical CPU(s)
     *  Identifier: Intel64 Family 6 Model 79 Stepping
     *  1\n ProcessorID: F1 06 04 00 FF FB EB BF
     *  Microarchitecture: Broadwell (Server)
     */
    private String cpuModel;
}

