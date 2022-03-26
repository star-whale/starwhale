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
import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
        return projectConvertor.convert(projectEntity);
    }

    /**
     * Get the list of projects.
     * @param project Search by project name prefix if the project name is set.
     * @param pageParams Paging parameters.
     * @return A list of ProjectVO objects
     */
    public List<ProjectVO> listProject(Project project, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ProjectEntity> entities = projectMapper.listProjects(project.getName());
        return entities.stream()
            .map(projectConvertor :: convert)
            .collect(Collectors.toList());
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
            .build();
        projectMapper.createProject(entity);
        return idConvertor.convert(entity.getId());
    }

    /**
     * Delete a project
     * @param project Project ID must be set.
     * @return Is the operation successful.
     */
    public Boolean deleteProject(Project project) {
        int res = projectMapper.deleteProject(idConvertor.revert(project.getId()));
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
        return res > 0;
    }

}
