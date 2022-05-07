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
import ai.starwhale.mlops.api.protocol.project.ProjectRequest;
import ai.starwhale.mlops.api.protocol.project.ProjectVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Project")
@Validated
public interface ProjectApi {

    @Operation(summary = "Get the list of projects")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "ok",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class)))})
    @GetMapping(value = "/project")
    ResponseEntity<ResponseMessage<PageInfo<ProjectVO>>> listProject(
        @Valid @RequestParam(value = "projectName", required = false) String projectName,
        @Parameter(in = ParameterIn.QUERY, description = "Id of the project owner", schema = @Schema())
        @Valid @RequestParam(value = "ownerId", required = false) String ownerId,
        @Parameter(in = ParameterIn.QUERY, description = "Name of the project owner", schema = @Schema())
        @Valid @RequestParam(value = "ownerName", required = false) String ownerName,
        @Valid @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
        @Valid @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);


    @Operation(summary = "Create a new project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/project")
    ResponseEntity<ResponseMessage<String>> createProject(
        @Valid @RequestBody ProjectRequest projectRequest);


    @Operation(summary = "Delete a project by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/project/{projectId}")
    ResponseEntity<ResponseMessage<String>> deleteProjectById(
        @Valid @PathVariable("projectId") String projectId);


    @Operation(summary = "Get a project by ID", description = "Returns a single project object.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "ok.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectVO.class)))})
    @GetMapping(value = "/project/{projectId}")
    ResponseEntity<ResponseMessage<ProjectVO>> getProjectById(
        @PathVariable("projectId") String projectId);


    @Operation(summary = "Modify project information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectId}")
    ResponseEntity<ResponseMessage<String>> updateProject(
        @PathVariable("projectId") String projectId,
        @Valid @RequestParam(value = "projectName", required = false) String projectName);


}
