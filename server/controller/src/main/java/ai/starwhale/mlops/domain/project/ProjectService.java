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

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private ProjectConvertor projectConvertor;

    /**
     * Find a project by parameters.
     * @param projectUrl Project URL must be set.
     * @return Optional of a ProjectVO object.
     */
    public ProjectVO findProject(String projectUrl) {
        Project project = projectManager.fromUrl(projectUrl);
        ProjectEntity projectEntity = projectManager.findProject(project);
        if(projectEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip("Unable to find project"), HttpStatus.BAD_REQUEST);
        }
        return projectConvertor.convert(projectEntity);
    }

    /**
     * Get the list of projects.
     * @param project Search by project name prefix if the project name is set.
     * @param pageParams Paging parameters.
     * @return A list of ProjectVO objects
     */
    public PageInfo<ProjectVO> listProject(Project project, PageParams pageParams, OrderParams orderParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ProjectEntity> entities = projectManager.listProjects(project, project.getOwner(), orderParams);

        return PageUtil.toPageInfo(entities, projectConvertor::convert);
    }

    /**
     * Create a new project
     * @param project Object of the project to create.
     * @return ID of the project was created.
     */
    public Long createProject(Project project) {
        if (projectManager.existProject(project.getName(), false)) {
            //项目存在且未被删除
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip(String.format("Project %s already exists", project.getName())), HttpStatus.BAD_REQUEST);
        }

        ProjectEntity entity = ProjectEntity.builder()
            .projectName(project.getName())
            .ownerId(project.getOwner().getId())
            .isDefault(project.isDefault() ? 1 : 0)
            .build();
        projectMapper.createProject(entity);
        log.info("Project has been created. ID={}, NAME={}", entity.getId(), entity.getProjectName());
        return entity.getId();
    }

    /**
     * Delete a project
     * @param projectUrl Project URL must be set.
     * @return Is the operation successful.
     */
    public Boolean deleteProject(String projectUrl) {
        Project project = projectManager.fromUrl(projectUrl);
        ProjectEntity entity = projectManager.findProject(project);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip("Unable to find project"), HttpStatus.BAD_REQUEST);
        }
        if(entity.getIsDefault() > 0) {
            throw new StarWhaleApiException(
                new SWValidationException(ValidSubject.PROJECT)
                    .tip("Default project cannot be deleted."), HttpStatus.BAD_REQUEST);
        }
        int res = projectMapper.deleteProject(entity.getId());
        log.info("Project has been deleted. ID={}", entity.getId());
        return res > 0;
    }

    public Boolean recoverProject(String projectUrl) {
        Project project = projectManager.fromUrl(projectUrl);
        String projectName = project.getName();
        Long id = project.getId();
        if(id != null) {
            ProjectEntity entity = projectMapper.findProject(project.getId());
            if(entity == null) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                    .tip("Recover project error. Project can not be found. "), HttpStatus.BAD_REQUEST);
            }
            projectName = entity.getProjectName();
        } else if (!StrUtil.isEmpty(projectName)) {
            // To restore projects by name, need to check whether there are duplicate names
            List<ProjectEntity> deletedProjects = projectMapper.listDeletedProjects(projectName);
            if(deletedProjects.size() > 1) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                    .tip(StrUtil.format("Recover project error. Duplicate names [%s] of deleted project. ", projectName)),
                    HttpStatus.BAD_REQUEST);
            } else if (deletedProjects.size() == 0) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                    .tip(StrUtil.format("Recover project error. Can not find deleted project [%s].", projectName)),
                    HttpStatus.BAD_REQUEST);
            }
            id = deletedProjects.get(0).getId();
        }

        // Check for duplicate names
        if(projectManager.existProject(projectName, false)) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip(String.format("Recover project error. Project %s already exists", projectName)), HttpStatus.BAD_REQUEST);
        }

        int res = projectMapper.recoverProject(id);
        log.info("Project has been recovered. Name={}", projectName);
        return res > 0;
    }

    /**
     * Modify a project. Now only project name can be modified
     * @param project Project object.
     * @return Is the operation successful.
     */
    public Boolean modifyProject(Project project) {
        ProjectEntity entity = ProjectEntity.builder()
            .id(project.getId())
            .projectName(project.getName())
            .build();
        int res = projectMapper.modifyProject(entity);
        log.info("Project has been modified ID={}", entity.getId());
        return res > 0;
    }

}
