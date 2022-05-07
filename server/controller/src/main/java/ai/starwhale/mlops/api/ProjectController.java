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

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.project.ProjectRequest;
import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.project.Project;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class ProjectController implements ProjectApi{

    @Resource
    private ProjectService projectService;

    @Resource
    private UserService userService;

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ProjectVO>>> listProject(String projectName,
        String ownerId, String ownerName, Integer pageNum, Integer pageSize) {

        PageInfo<ProjectVO> projects = projectService.listProject(
            Project.builder()
                .name(projectName)
                .owner(User.builder().id(ownerId).name(ownerName).build())
                .build(),
            PageParams.builder().pageNum(pageNum).pageSize(pageSize).build());


        return ResponseEntity.ok(Code.success.asResponse(projects));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createProject(ProjectRequest projectRequest) {
        User user = userService.currentUserDetail();

        String projectId = projectService
            .createProject(Project.builder()
                .name(projectRequest.getProjectName())
                .owner(User.builder().id(user.getId()).build())
                .isDefault(false)
                .build());

        return ResponseEntity.ok(Code.success.asResponse(projectId));

    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteProjectById(String projectId) {
        Boolean res = projectService.deleteProject(Project.builder().id(projectId).build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Delete project failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<ProjectVO>> getProjectById(String projectId) {
        ProjectVO project = projectService.findProject(Project.builder().id(projectId).build());
        return ResponseEntity.ok(Code.success.asResponse(project));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateProject(String projectId,
        String projectName) {
        Boolean res = projectService
            .modifyProject(Project.builder()
                .id(projectId)
                .name(projectName)
                .build());
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update project failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
