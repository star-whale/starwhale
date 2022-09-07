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
import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.api.protocol.project.UpdateProjectRequest;
import ai.starwhale.mlops.api.protocol.user.ProjectRoleVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class ProjectController implements ProjectApi{

    private final ProjectService projectService;

    private final UserService userService;

    private final IDConvertor idConvertor;

    public ProjectController(ProjectService projectService, UserService userService,
        IDConvertor idConvertor) {
        this.projectService = projectService;
        this.userService = userService;
        this.idConvertor = idConvertor;
    }


    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ProjectVO>>> listProject(String projectName,
        Integer pageNum, Integer pageSize, String sort, Integer order) {
        User user = userService.currentUserDetail();
        PageInfo<ProjectVO> projects = projectService.listProject(
            projectName,
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build(),
            OrderParams.builder()
                .sort(sort)
                .order(order)
                .build(),
            user);

        return ResponseEntity.ok(Code.success.asResponse(projects));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createProject(
        CreateProjectRequest createProjectRequest) {
        Long projectId = projectService
                .createProject(Project.builder()
                    .name(createProjectRequest.getProjectName())
                    .owner(User.builder()
                        .id(idConvertor.revert(createProjectRequest.getOwnerId()))
                        .build())
                    .isDefault(false)
                    .privacy(Privacy.fromName(createProjectRequest.getPrivacy()))
                    .description(createProjectRequest.getDescription())
                    .build());

        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(projectId)));

    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteProjectByUrl(String projectUrl) {
        Boolean res = projectService.deleteProject(projectUrl);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete project failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverProject(String projectId) {
        projectService.recoverProject(projectId);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<ProjectVO>> getProjectByUrl(String projectUrl) {
        ProjectVO vo = projectService.findProject(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateProject(String projectUrl,
        UpdateProjectRequest updateProjectRequest) {
        Boolean res = projectService.modifyProject(projectUrl,
            updateProjectRequest.getProjectName(),
            updateProjectRequest.getDescription(),
            idConvertor.revert(updateProjectRequest.getOwnerId()),
            updateProjectRequest.getPrivacy()
        );
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update project failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<List<ProjectRoleVO>>> listProjectRole(String projectUrl) {
        List<ProjectRoleVO> vos = projectService.listProjectRoles(projectUrl);
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> addProjectRole(String projectUrl, String userId,
        String roleId) {
        Boolean res = projectService.addProjectRole(projectUrl, idConvertor.revert(userId),
            idConvertor.revert(roleId));
        if(!res) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT).tip("Add project role failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteProjectRole(String projectUrl,
        String projectRoleId) {
        Boolean res = projectService.deleteProjectRole(projectUrl, idConvertor.revert(projectRoleId));
        if(!res) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT).tip("Delete project role failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyProjectRole(String projectUrl,
        String projectRoleId, String roleId) {
        Boolean res = projectService.modifyProjectRole(projectUrl, idConvertor.revert(projectRoleId),
            idConvertor.revert(roleId));
        if(!res) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT).tip("Modify project role failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
