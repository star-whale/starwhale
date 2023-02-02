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

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Panel")
@Validated
public interface PanelApi {
    @Operation(summary = "Install a plugin", description = "Upload a tarball and install as panel plugin")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/panel/plugin", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> installPlugin(
            @Parameter(description = "file detail") @RequestPart(value = "file") MultipartFile file);

    @Operation(summary = "Uninstall a plugin", description = "Uninstall plugin by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/panel/plugin/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> uninstallPlugin(
            @Parameter(in = ParameterIn.PATH, description = "Plugin id", schema = @Schema())
            @PathVariable("id")
            String id
    );

    @Operation(summary = "List all plugins", description = "List all plugins")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/panel/plugin", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<PanelPluginVo>>> pluginList();

    @Operation(summary = "Get panel setting", description = "Get panel setting by project and key")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/panel/setting/{project_id}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<String>> getPanelSetting(
            @Parameter(in = ParameterIn.PATH, description = "Project id", schema = @Schema())
            @PathVariable("project_id")
            String projectId,

            @Parameter(in = ParameterIn.PATH, description = "Setting key", schema = @Schema())
            @PathVariable("key")
            String key
    );

    @Operation(summary = "Save panel setting", description = "Save panel setting by project and key")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/panel/setting/{project_id}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> setPanelSetting(
            @Parameter(in = ParameterIn.PATH, description = "Project id", schema = @Schema())
            @PathVariable("project_id")
            String projectId,

            @Parameter(in = ParameterIn.PATH, description = "Setting key", schema = @Schema())
            @PathVariable("key")
            String key,

            @RequestBody
            String content
    );
}
