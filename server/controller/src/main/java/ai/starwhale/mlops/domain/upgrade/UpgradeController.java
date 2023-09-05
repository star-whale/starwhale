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

package ai.starwhale.mlops.domain.upgrade;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.system.LatestVersionVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeRequest;
import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import ai.starwhale.mlops.domain.upgrade.bo.Version;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@ConditionalOnProperty("sw.upgrade.enabled")
public class UpgradeController {

    private final UpgradeService upgradeService;

    public UpgradeController(UpgradeService upgradeService) {
        this.upgradeService = upgradeService;
    }

    @Operation(summary = "Upgrade system version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/system/version/upgrade",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<ResponseMessage<String>> upgradeVersion(@Valid @RequestBody UpgradeRequest upgradeRequest) {
        upgradeService.upgrade(new Version(upgradeRequest.getVersion(), upgradeRequest.getImage()));
        return ResponseEntity.ok(Code.success.asResponse("Preparing for upgrade."));
    }


    @Operation(summary = "Cancel upgrading")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/system/version/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<ResponseMessage<String>> cancelUpgrading() {
        upgradeService.cancelUpgrade();
        return ResponseEntity.ok(Code.success.asResponse("Upgrading has been cancelled."));
    }

    @Operation(summary = "Get latest version of the system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/system/version/latest",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<LatestVersionVo>> getLatestVersion() {
        Version latestVersion = upgradeService.getLatestVersion();
        LatestVersionVo version = LatestVersionVo.builder()
                .version(latestVersion.getNumber())
                .image(latestVersion.getImage())
                .build();
        return ResponseEntity.ok(Code.success.asResponse(version));
    }

    @Operation(
            summary = "Get the current upgrade progress",
            description =
                    "Get the current server upgrade process. If downloading, return the download progress")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/system/version/progress",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<ResponseMessage<List<UpgradeLog>>> getUpgradeProgress() {
        List<UpgradeLog> upgradeLog = upgradeService.getUpgradeLog();
        return ResponseEntity.ok(Code.success.asResponse(upgradeLog));
    }


}
