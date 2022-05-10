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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVO;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "System")
@Validated
public interface SystemApi {

    @Operation(summary = "Get the list of agents")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageInfo.class)))
        })
    @GetMapping(value = "/system/agent")
    ResponseEntity<ResponseMessage<PageInfo<AgentVO>>> listAgent(
        @Parameter(in = ParameterIn.QUERY, description = "Agent ip to search for", schema = @Schema())
        @Valid
        @RequestParam(value = "ip", required = false)
            String ip,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(summary = "remove offline agent")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
        })
    @DeleteMapping(value = "/system/agent")
    ResponseEntity<ResponseMessage<String>> deleteAgent(
        @Parameter(in = ParameterIn.QUERY, description = "the serialNumber of the agent to be deleted", schema = @Schema())
        @Valid
        @RequestParam(value = "serialNumber", required = true)
            String serialNumber);

    @Operation(summary = "Upgrade system version or cancel upgrade")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/system/version/{action}")
    ResponseEntity<ResponseMessage<String>> systemVersionAction(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Action: upgrade or cancel",
            required = true,
            schema = @Schema(allowableValues = {"upgrade", "cancel"}))
        @PathVariable("action")
            String action);

    @Operation(summary = "Get current version of the system")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SystemVersionVO.class)))
        })
    @GetMapping(value = "/system/version")
    ResponseEntity<ResponseMessage<SystemVersionVO>> getCurrentVersion();

    @Operation(summary = "Get latest version of the system")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SystemVersionVO.class)))
        })
    @GetMapping(value = "/system/version/latest")
    ResponseEntity<ResponseMessage<SystemVersionVO>> getLatestVersion();

    @Operation(
        summary = "Get the current upgrade progress",
        description =
            "Get the current server upgrade process. If downloading, return the download progress")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpgradeProgressVO.class)))
        })
    @GetMapping(value = "/system/version/progress")
    ResponseEntity<ResponseMessage<UpgradeProgressVO>> getUpgradeProgress();
}
