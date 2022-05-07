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
