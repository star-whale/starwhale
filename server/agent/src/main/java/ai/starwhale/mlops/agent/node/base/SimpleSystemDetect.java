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
