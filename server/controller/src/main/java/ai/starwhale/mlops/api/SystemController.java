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
import ai.starwhale.mlops.api.protocol.system.ResourcePoolVo;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVo.PhaseEnum;
import ai.starwhale.mlops.domain.system.SystemService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class SystemController implements SystemApi {

    private final SystemService systemService;

    private final SystemSettingService systemSettingService;

    public SystemController(SystemService systemService,
            SystemSettingService systemSettingService) {
        this.systemService = systemService;
        this.systemSettingService = systemSettingService;
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ResourcePoolVo>>> listResourcePools() {
        return ResponseEntity.ok(Code.success.asResponse(systemService.listResourcePools()));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> systemVersionAction(String action) {
        return ResponseEntity.ok(Code.success.asResponse("Unknown action"));
    }

    @Override
    public ResponseEntity<ResponseMessage<SystemVersionVo>> getCurrentVersion() {
        SystemVersionVo version = SystemVersionVo.builder()
                .version(systemService.controllerVersion())
                .id("")
                .build();
        return ResponseEntity.ok(Code.success.asResponse(version));
    }

    @Override
    public ResponseEntity<ResponseMessage<SystemVersionVo>> getLatestVersion() {
        SystemVersionVo version = SystemVersionVo.builder()
                .version("mvp")
                .id("")
                .build();
        return ResponseEntity.ok(Code.success.asResponse(version));
    }

    @Override
    public ResponseEntity<ResponseMessage<UpgradeProgressVo>> getUpgradeProgress() {
        UpgradeProgressVo progress = UpgradeProgressVo.builder()
                .phase(PhaseEnum.DOWNLOADING)
                .progress(99)
                .build();
        return ResponseEntity.ok(Code.success.asResponse(progress));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateSetting(String setting) {
        return ResponseEntity.ok(Code.success.asResponse(systemSettingService.updateSetting(setting)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> querySetting() {
        return ResponseEntity.ok(Code.success.asResponse(systemSettingService.querySetting()));
    }
}
