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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "System")
@RequestMapping("${sw.controller.api-prefix}")
public class SystemController {

    private final SystemService systemService;

    private final SystemSettingService systemSettingService;

    public SystemController(SystemService systemService,
                            SystemSettingService systemSettingService) {
        this.systemService = systemService;
        this.systemSettingService = systemSettingService;
    }

    @Operation(summary = "Get the list of resource pool")
    @GetMapping(value = "/system/resourcePool", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<List<ResourcePool>>> listResourcePools() {
        return ResponseEntity.ok(Code.success.asResponse(systemService.listResourcePools()));
    }

    @Operation(summary = "Update resource pool")
    @PostMapping(value = "/system/resourcePool", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateResourcePools(
            @Valid @RequestBody List<ResourcePool> resourcePools
    ) {
        systemService.updateResourcePools(resourcePools);
        return ResponseEntity.ok(Code.success.asResponse("Resource pools updated."));
    }

    @Operation(summary = "Get current version of the system")
    @GetMapping(value = "/system/version", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<SystemVersionVo>> getCurrentVersion() {
        SystemVersionVo version = SystemVersionVo.builder()
                .version(systemService.controllerVersion())
                .id("")
                .build();
        return ResponseEntity.ok(Code.success.asResponse(version));
    }

    @Operation(summary = "Update system settings", description = "Update system settings")
    @PostMapping(value = "/system/setting", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> updateSetting(@RequestBody String setting) {
        return ResponseEntity.ok(Code.success.asResponse(systemSettingService.updateSetting(setting)));
    }

    @Operation(summary = "Get system settings", description = "Get system settings in yaml string")
    @GetMapping(value = "/system/setting", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> querySetting() {
        return ResponseEntity.ok(Code.success.asResponse(systemSettingService.querySetting()));
    }

    @Operation(summary = "Get system features", description = "Get system features list")
    @GetMapping(value = "/system/features", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<FeaturesVo>> queryFeatures() {
        return ResponseEntity.ok(Code.success.asResponse(systemService.queryFeatures()));
    }
}
