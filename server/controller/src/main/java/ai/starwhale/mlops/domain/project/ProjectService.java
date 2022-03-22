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
import java.util.Optional;
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
     * @param projectVO Project ID must be set.
     * @return Optional of a ProjectVO object.
     */
    public ProjectVO findProject(ProjectVO projectVO) {
        ProjectEntity projectEntity = projectMapper.findProject(idConvertor.revert(projectVO.getId()));
        return projectConvertor.convert(projectEntity);
    }

    /**
     * Get the list of projects.
     * @param projectVO Search by project name prefix if the project name is set.
     * @param pageParams Paging parameters.
     * @return A list of ProjectVO objects
     */
    public List<ProjectVO> listProject(ProjectVO projectVO, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ProjectEntity> entities = projectMapper.listProjects(projectVO.getName());
        return entities.stream()
            .map(entity -> projectConvertor.convert(entity))
            .collect(Collectors.toList());
    }

    /**
     * Create a new project
     * @param projectVO Object of the project to create.
     * @return ID of the project was created.
     */
    public Long createProject(ProjectVO projectVO) {
        Long newId = projectMapper.createProject(projectConvertor.revert(projectVO));
        return newId;
    }

    /**
     * Delete a project
     * @param projectVO Project ID must be set.
     * @return Is the operation successful.
     */
    public Boolean deleteProject(ProjectVO projectVO) {
        int res = projectMapper.deleteProject(idConvertor.revert(projectVO.getId()));
        return res > 0;
    }

    /**
     * Modify a project. Now only project name can be modified
     * @param projectVO Project object.
     * @return Is the operation successful.
     */
    public Boolean modifyProject(ProjectVO projectVO) {
        int res = projectMapper.modifyProject(projectConvertor.revert(projectVO));
        return res > 0;
    }

}
