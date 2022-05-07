/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.cpu;

import cn.hutool.system.oshi.OshiUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * detect CPU of the machine,by native image(dependency direct running on the host)
 */
@Slf4j
public class SimpleCPUDetect implements CPUDetect {

    public Optional<CPUInfo> detect() {
        try {
            cn.hutool.system.oshi.CpuInfo cpuInfo = OshiUtil.getCpuInfo();
            return Optional.of(
                CPUInfo.builder()
                    .total(cpuInfo.getToTal())
                    .cpuNum(cpuInfo.getCpuNum())
                    .free(cpuInfo.getFree())
                    .sys(cpuInfo.getSys())
                    .user(cpuInfo.getUser())
                    .wait(cpuInfo.getWait())
                    .cpuModel(cpuInfo.getCpuModel())
                    .build()
            );
        } catch (Exception e) {
            log.error("detect cpu info occur error, message: {}", e.getMessage(), e);
        }
        return Optional.empty();

    }
}
