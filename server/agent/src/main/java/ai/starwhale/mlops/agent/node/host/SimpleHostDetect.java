/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.host;

import cn.hutool.system.SystemUtil;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleHostDetect implements HostDetect {

    @Override
    public Optional<HostInfo> detect() {
        cn.hutool.system.HostInfo hostInfo = SystemUtil.getHostInfo();
        try{
            return Optional.of(
                HostInfo.builder()
                    .hostAddress(hostInfo.getAddress())
                    .hostName(hostInfo.getName())
                    .build()
            );
        } catch (Exception e) {
            log.error("detect host info occur error, message: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }
}
