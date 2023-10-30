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
import ai.starwhale.mlops.api.protocol.project.CreateProjectRequest;
import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.project.UpdateProjectRequest;
import ai.starwhale.mlops.api.protocol.user.ProjectMemberVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "Project")
@RequestMapping("${sw.controller.api-prefix}")
public class ProjectController {

    private final ProjectService projectService;

    private final UserService userService;

    private final MemberService memberService;

    private final IdConverter idConvertor;

    public ProjectController(
            ProjectService projectService, UserService userService,
            MemberService memberService, IdConverter idConvertor
    ) {
        this.projectService = projectService;
        this.userService = userService;
        this.memberService = memberService;
        this.idConvertor = idConvertor;
    }

    @Operation(summary = "Get the list of projects")
    @GetMapping(value = "/project", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ProjectVo>>> listProject(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The sort type of project list. (Default=visited)",
                    schema = @Schema(allowableValues = {"visited", "latest", "oldest"}))
            @RequestParam(required = false) String sort
    ) {
        User user = userService.currentUserDetail();
        PageInfo<ProjectVo> projects = projectService.listProject(
                projectName,
                OrderParams.builder()
                        .sort(sort)
                        .build(),
                user
        );

        return ResponseEntity.ok(Code.success.asResponse(projects));
    }

    @Operation(summary = "Create or Recover a new project")
    @PostMapping(value = "/project", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> createProject(
            @Valid @RequestBody CreateProjectRequest createProjectRequest
    ) {
        var user = userService.currentUserDetail().getId();
        Long projectId = projectService
                .createProject(Project.builder()
                        .name(createProjectRequest.getProjectName())
                        .owner(User.builder()
                                .id(user)
                                .build())
                        .isDefault(false)
                        .privacy(Privacy.fromName(createProjectRequest.getPrivacy()))
                        .description(createProjectRequest.getDescription())
                        .overview(createProjectRequest.getOverview())
                        .build());

        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(projectId)));

    }

    @Operation(summary = "Delete a project by Url")
    @DeleteMapping(value = "/project/{projectUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> deleteProjectByUrl(
            @PathVariable String projectUrl
    ) {
        Boolean res = projectService.deleteProject(projectUrl);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Delete project failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Recover a project")
    @PutMapping(value = "/project/{projectId}/recover", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverProject(
            @PathVariable String projectId
    ) {
        projectService.recoverProject(projectId);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Get a project by Url", description = "Returns a single project object.")
    @GetMapping(value = "/project/{projectUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ProjectVo>> getProjectByUrl(
            @PathVariable String projectUrl
    ) {
        projectService.visit(projectUrl);
        ProjectVo vo = projectService.getProjectVo(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vo));
    }

    @Operation(summary = "Modify project information")
    @PutMapping(value = "/project/{projectUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> updateProject(
            @PathVariable String projectUrl,
            @Valid @RequestBody UpdateProjectRequest updateProjectRequest
    ) {
        Boolean res = projectService.updateProject(
                projectUrl,
                updateProjectRequest.getProjectName(),
                updateProjectRequest.getDescription(),
                updateProjectRequest.getOverview(),
                updateProjectRequest.getPrivacy()
        );
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Update project failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "List project roles")
    @GetMapping(value = "/project/{projectUrl}/role", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<ProjectMemberVo>>> listProjectRole(
            @PathVariable String projectUrl
    ) {
        List<ProjectMemberVo> vos = projectService.listProjectMembersInProject(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Operation(summary = "Grant project role to a user")
    @PostMapping(value = "/project/{projectUrl}/role", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> addProjectRole(
            @PathVariable String projectUrl,
            @RequestParam String userId,
            @RequestParam String roleId
    ) {
        Boolean res = projectService.addProjectMember(projectUrl, idConvertor.revert(userId),
                idConvertor.revert(roleId)
        );
        if (!res) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT, "Add project role failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Delete a project role")
    @DeleteMapping(value = "/project/{projectUrl}/role/{projectRoleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> deleteProjectRole(
            @PathVariable String projectUrl,
            @PathVariable String projectRoleId
    ) {
        Boolean res = memberService.deleteProjectMember(idConvertor.revert(projectRoleId));
        if (!res) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT, "Delete project role failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Modify a project role")
    @PutMapping(value = "/project/{projectUrl}/role/{projectRoleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> modifyProjectRole(
            @PathVariable String projectUrl,
            @PathVariable String projectRoleId,
            @RequestParam String roleId
    ) {
        Boolean res = memberService.modifyProjectMember(
                idConvertor.revert(projectRoleId),
                idConvertor.revert(roleId)
        );
        if (!res) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT, "Modify project role failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
