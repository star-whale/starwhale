/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
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
        @Valid @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize);


    @Operation(summary = "Create a new project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/project")
    ResponseEntity<ResponseMessage<String>> createProject(
        @Valid @RequestParam(value = "projectName") String projectName);


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
