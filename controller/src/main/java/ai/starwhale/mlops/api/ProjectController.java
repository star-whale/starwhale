/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.doman.project.ProjectVO;
import com.github.pagehelper.PageInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectController implements ProjectApi{

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<ProjectVO>>> listProject(String projectName,
        Integer pageNum, Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createProject(String projectName) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteProjectById(String projectId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<ProjectVO>> getProjectById(String projectId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateProject(String projectId,
        String projectName) {
        return null;
    }
}
