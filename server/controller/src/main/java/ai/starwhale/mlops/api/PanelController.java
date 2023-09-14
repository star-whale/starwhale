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
import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import ai.starwhale.mlops.domain.panel.PanelSettingService;
import ai.starwhale.mlops.domain.panel.PluginService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@Tag(name = "Panel")
@RequestMapping("${sw.controller.api-prefix}")
public class PanelController {

    private final PluginService pluginService;
    private final PanelSettingService panelSettingService;

    PanelController(PluginService pluginService, PanelSettingService panelSettingService) {
        this.pluginService = pluginService;
        this.panelSettingService = panelSettingService;
    }

    @Operation(summary = "Install a plugin", description = "Upload a tarball and install as panel plugin")
    @PostMapping(value = "/panel/plugin", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> installPlugin(
            @Parameter(description = "file detail") @RequestPart(value = "file") MultipartFile file
    ) {
        pluginService.installPlugin(file);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "Uninstall a plugin", description = "Uninstall plugin by id")
    @DeleteMapping(value = "/panel/plugin/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> uninstallPlugin(
            @PathVariable String id
    ) {
        pluginService.uninstallPlugin(id);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "List all plugins", description = "List all plugins")
    @GetMapping(value = "/panel/plugin", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<PanelPluginVo>>> pluginList() {
        // TODO support page params
        return ResponseEntity.ok(Code.success.asResponse(pluginService.listPlugin()));
    }

    @Operation(summary = "Get panel setting", description = "Get panel setting by project and key")
    @GetMapping(value = "/panel/setting/{projectUrl}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> getPanelSetting(
            @PathVariable String projectUrl,
            @PathVariable String key
    ) {
        return ResponseEntity.ok(Code.success.asResponse(panelSettingService.getSetting(projectUrl, key)));
    }

    @Operation(summary = "Save panel setting", description = "Save panel setting by project and key")
    @PostMapping(value = "/panel/setting/{projectUrl}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> setPanelSetting(
            @PathVariable String projectUrl,
            @PathVariable String key,
            @RequestBody String content
    ) {
        panelSettingService.saveSetting(projectUrl, key, content);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }
}
