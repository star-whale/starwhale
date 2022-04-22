/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.stream.Collectors;
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
    private IDConvertor idConvertor;

    @Resource
    private ProjectConvertor projectConvertor;

    /**
     * Find a project by parameters.
     * @param project Project ID must be set.
     * @return Optional of a ProjectVO object.
     */
    public ProjectVO findProject(Project project) {
        ProjectEntity projectEntity = projectMapper.findProject(idConvertor.revert(project.getId()));
        if(projectEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip(String.format("Unable to find project %s", project.getId())), HttpStatus.BAD_REQUEST);
        }
        return projectConvertor.convert(projectEntity);
    }

    /**
     * Get the list of projects.
     * @param project Search by project name prefix if the project name is set.
     * @param pageParams Paging parameters.
     * @return A list of ProjectVO objects
     */
    public PageInfo<ProjectVO> listProject(Project project, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ProjectEntity> entities = projectMapper.listProjects(project.getName());
        return PageUtil.toPageInfo(entities, projectConvertor::convert);
    }

    /**
     * Create a new project
     * @param project Object of the project to create.
     * @return ID of the project was created.
     */
    public String createProject(Project project) {
        ProjectEntity entity = ProjectEntity.builder()
            .projectName(project.getName())
            .ownerId(idConvertor.revert(project.getOwnerId()))
            .isDefault(project.isDefault() ? 1 : 0)
            .build();
        projectMapper.createProject(entity);
        log.info("Project has been created. ID={}, NAME={}", entity.getId(), entity.getProjectName());
        return idConvertor.convert(entity.getId());
    }

    /**
     * Delete a project
     * @param project Project ID must be set.
     * @return Is the operation successful.
     */
    public Boolean deleteProject(Project project) {
        Long id = idConvertor.revert(project.getId());
        ProjectEntity entity = projectMapper.findProject(id);
        if(entity.getIsDefault() > 0) {
            throw new StarWhaleApiException(
                new SWValidationException(ValidSubject.PROJECT)
                    .tip("Default project cannot be deleted."), HttpStatus.BAD_REQUEST);
        }
        int res = projectMapper.deleteProject(id);
        log.info("Project has been deleted. ID={}", entity.getId());
        return res > 0;
    }

    /**
     * Modify a project. Now only project name can be modified
     * @param project Project object.
     * @return Is the operation successful.
     */
    public Boolean modifyProject(Project project) {
        ProjectEntity entity = ProjectEntity.builder()
            .id(idConvertor.revert(project.getId()))
            .projectName(project.getName())
            .build();
        int res = projectMapper.modifyProject(entity);
        log.info("Project has been modified ID={}", entity.getId());
        return res > 0;
    }

}
