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
import ai.starwhale.mlops.api.protocol.system.FeaturesVo;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVo;
import ai.starwhale.mlops.domain.system.SystemService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.api-prefix}")
public class SystemController implements SystemApi {

    private final SystemService systemService;

    private final SystemSettingService systemSettingService;

    public SystemController(SystemService systemService,
                            SystemSettingService systemSettingService) {
        this.systemService = systemService;
        this.systemSettingService = systemSettingService;
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ResourcePool>>> listResourcePools() {
        return ResponseEntity.ok(Code.success.asResponse(systemService.listResourcePools()));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateResourcePools(List<ResourcePool> resourcePools) {
        systemService.updateResourcePools(resourcePools);
        return ResponseEntity.ok(Code.success.asResponse("Resource pools updated."));
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
    public ResponseEntity<ResponseMessage<String>> updateSetting(String setting) {
        return ResponseEntity.ok(Code.success.asResponse(systemSettingService.updateSetting(setting)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> querySetting() {
        return ResponseEntity.ok(Code.success.asResponse(systemSettingService.querySetting()));
    }

    @Override
    public ResponseEntity<ResponseMessage<FeaturesVo>> queryFeatures() {
        return ResponseEntity.ok(Code.success.asResponse(systemService.queryFeatures()));
    }
}
