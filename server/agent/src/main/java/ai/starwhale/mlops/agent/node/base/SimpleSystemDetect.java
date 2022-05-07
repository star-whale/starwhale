/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.base;

import cn.hutool.system.SystemUtil;
import cn.hutool.system.oshi.OshiUtil;
import lombok.extern.slf4j.Slf4j;
import oshi.hardware.GlobalMemory;

import java.util.Optional;

@Slf4j
public class SimpleSystemDetect implements SystemDetect {

    @Override
    public Optional<SystemInfo> detect() {
        try{
            cn.hutool.system.HostInfo hostInfo = SystemUtil.getHostInfo();

            GlobalMemory memory = OshiUtil.getMemory();
            return Optional.of(
                SystemInfo.builder()
                    .hostAddress(hostInfo.getAddress())
                    .hostName(hostInfo.getName())
                    .totalMemory(memory.getTotal())
                    .availableMemory(memory.getAvailable())
                    .build()
            );
        } catch (Exception e) {
            log.error("detect host info occur error, message: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }
}
