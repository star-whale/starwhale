/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
        Integer pageNum, Integer pageSize) {

        PageInfo<ProjectVO> projects = projectService.listProject(
            Project.builder().name(projectName).build(),
            PageParams.builder().pageNum(pageNum).pageSize(pageSize).build());


        return ResponseEntity.ok(Code.success.asResponse(projects));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createProject(ProjectRequest projectRequest) {
        User user = userService.currentUserDetail();

        String projectId = projectService
            .createProject(Project.builder()
                .name(projectRequest.getProjectName())
                .ownerId(user.getId())
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
