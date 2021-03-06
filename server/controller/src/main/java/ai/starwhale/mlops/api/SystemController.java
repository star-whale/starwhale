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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVO;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVO;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVO.PhaseEnum;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.system.SystemService;
import com.github.pagehelper.PageInfo;
import javax.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class SystemController implements SystemApi{

    @Resource
    private SystemService systemService;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<AgentVO>>> listAgent(String ip, Integer pageNum,
        Integer pageSize) {
        PageParams pageParams = PageParams.builder().pageNum(pageNum).pageSize(pageSize).build();
        PageInfo<AgentVO> pageInfo = systemService.listAgents(ip, pageParams);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteAgent(String serialNumber) {
        return ResponseEntity.ok(Code.success.asResponse(systemService.deleteOfflineAgent(serialNumber)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> systemVersionAction(String action) {
        return ResponseEntity.ok(Code.success.asResponse("Unknown action"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SystemVersionVO>> getCurrentVersion() {
        SystemVersionVO version = SystemVersionVO.builder()
            .version(systemService.controllerVersion())
            .id("")
            .build();
        return ResponseEntity.ok(Code.success.asResponse(version));
    }

    @Override
    public ResponseEntity<ResponseMessage<SystemVersionVO>> getLatestVersion() {
        SystemVersionVO version = SystemVersionVO.builder()
            .version("mvp")
            .id("")
            .build();
        return ResponseEntity.ok(Code.success.asResponse(version));
    }

    @Override
    public ResponseEntity<ResponseMessage<UpgradeProgressVO>> getUpgradeProgress() {
        UpgradeProgressVO progress = UpgradeProgressVO.builder()
            .phase(PhaseEnum.DOWNLOADING)
            .progress(99)
            .build();
        return ResponseEntity.ok(Code.success.asResponse(progress));
    }
}
