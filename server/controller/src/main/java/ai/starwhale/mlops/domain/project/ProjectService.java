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
import ai.starwhale.mlops.api.protocol.project.StatisticsVO;
import ai.starwhale.mlops.api.protocol.user.ProjectRoleVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectRoleMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.Page.Function;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private ProjectConvertor projectConvertor;

    @Resource
    private ProjectRoleMapper projectRoleMapper;

    @Resource
    private ProjectRoleConvertor projectRoleConvertor;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserService userService;

    private static final String DELETE_SUFFIX = ".deleted";

    /**
     * Find a project by parameters.
     * @param projectUrl Project URL must be set.
     * @return Optional of a ProjectVO object.
     */
    public ProjectVO findProject(String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectEntity projectEntity = projectMapper.findProject(projectId);
        if(projectEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip("Unable to find project"), HttpStatus.BAD_REQUEST);
        }
        return projectConvertor.convert(projectEntity);
    }

    /**
     * Get the list of projects.
     * @param projectName Search by project name prefix if the project name is set.
     * @param pageParams Paging parameters.
     * @return A list of ProjectVO objects
     */
    public PageInfo<ProjectVO> listProject(String projectName, PageParams pageParams, OrderParams orderParams, User user) {
        Long userId = user.getId();
        List<Role> sysRoles = userService.getProjectRolesOfUser(user, "0");
        for (Role sysRole : sysRoles) {
            if(sysRole.getAuthority().equals("OWNER")) {
                userId = null;
                break;
            }
        }

        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ProjectEntity> entities = projectManager.listProjects(projectName, userId, orderParams);
        List<Long> ids = entities.stream().map(ProjectEntity::getId).collect(Collectors.toList());
        Map<Long, ProjectObjectCountEntity> countMap = projectManager.getObjectCountsOfProjects(
            ids);

        return PageUtil.toPageInfo(entities, entity -> {
            ProjectVO vo = projectConvertor.convert(entity);
            ProjectObjectCountEntity count = countMap.get(entity.getId());
            if (count != null) {
                vo.setStatistics(StatisticsVO.builder()
                        .modelCounts(Optional.ofNullable(count.getCountModel()).orElse(0))
                        .datasetCounts(Optional.ofNullable(count.getCountDataset()).orElse(0))
                        .memberCounts(Optional.ofNullable(count.getCountMember()).orElse(0))
                        .evaluationCounts(Optional.ofNullable(count.getCountJobs()).orElse(0))
                        .build());
            }
            return vo;
        });
    }

    /**
     * Create a new project
     * @param project Object of the project to create.
     * @return ID of the project was created.
     */
    public Long createProject(Project project) {
        Assert.notNull(project.getName(), "Project name must not be null");
        if (projectManager.existProject(project.getName())) {
            //项目存在且未被删除
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip(String.format("Project %s already exists", project.getName())), HttpStatus.BAD_REQUEST);
        }

        ProjectEntity entity = ProjectEntity.builder()
            .projectName(project.getName())
            .ownerId(project.getOwner().getId())
            .privacy(project.getPrivacy().getValue())
            .description(project.getDescription())
            .isDefault(project.isDefault() ? 1 : 0)
            .build();
        projectMapper.createProject(entity);
        projectRoleMapper.addProjectRoleByName(entity.getOwnerId(), entity.getId(), "owner");
        log.info("Project has been created. ID={}, NAME={}", entity.getId(), entity.getProjectName());
        return entity.getId();
    }

    /**
     * Delete a project
     * @param projectUrl Project URL must be set.
     * @return Is the operation successful.
     */
    @Transactional
    public Boolean deleteProject(String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectEntity entity = projectMapper.findProject(projectId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip("Unable to find project"), HttpStatus.BAD_REQUEST);
        }
        if(entity.getIsDefault() > 0) {
            throw new StarWhaleApiException(
                new SWValidationException(ValidSubject.PROJECT)
                    .tip("Default project cannot be deleted."), HttpStatus.BAD_REQUEST);
        }
        entity.setProjectName(entity.getProjectName() + DELETE_SUFFIX + "." + entity.getId());
        projectMapper.modifyProject(entity);
        int res = projectMapper.deleteProject(entity.getId());
        log.info("Project has been deleted. ID={}", entity.getId());
        return res > 0;
    }

    @Transactional
    public Long recoverProject(String projectUrl) {
        String projectName = projectUrl;
        Long id;
        if(idConvertor.isID(projectUrl)) {
            id = idConvertor.revert(projectUrl);
            ProjectEntity entity = projectMapper.findProject(id);
            if(entity == null) {
                throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                    .tip("Recover project error. Project can not be found. "), HttpStatus.BAD_REQUEST);
            }
            projectName = entity.getProjectName().substring(0,
                entity.getProjectName().lastIndexOf(DELETE_SUFFIX));
            entity.setProjectName(projectName);
        } else {
            // To restore projects by name, need to check whether there are duplicate names
            List<ProjectEntity> deletedProjects = projectMapper.listProjects(projectName + DELETE_SUFFIX, null, 1, null);
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
        if(projectManager.existProject(projectName)) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip(String.format("Recover project error. Project %s already exists", projectName)), HttpStatus.BAD_REQUEST);
        }
        projectMapper.modifyProject(ProjectEntity.builder()
            .id(id)
            .projectName(projectName)
            .build());
        projectMapper.recoverProject(id);
        log.info("Project has been recovered. Name={}", projectName);
        return id;
    }

    public Boolean modifyProject(String projectUrl, String projectName, String description, Long userId, String privacy) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectEntity entity = ProjectEntity.builder()
            .id(projectId)
            .projectName(projectName)
            .description(description)
            .ownerId(userId)
            .privacy(Privacy.fromName(privacy).getValue())
            .build();
        int res = projectMapper.modifyProject(entity);
        log.info("Project has been modified ID={}", entity.getId());
        return res > 0;
    }

    public List<ProjectRoleVO> listProjectRoles(String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        List<ProjectRoleEntity> entities = projectRoleMapper.listProjectRoles(projectId);
        return entities.stream()
            .map(projectRoleConvertor::convert)
            .collect(Collectors.toList());
    }

    public Boolean addProjectRole(String projectUrl, Long userId, Long roleId) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectRoleEntity entity = ProjectRoleEntity.builder()
            .userId(userId)
            .roleId(roleId)
            .projectId(projectId)
            .build();
        int res = projectRoleMapper.addProjectRole(entity);
        log.info("Project Role has been created ID={}", entity.getId());
        return res > 0;
    }

    public Boolean modifyProjectRole(String projectUrl, Long projectRoleId, Long roleId) {
        int res = projectRoleMapper.updateProjectRole(ProjectRoleEntity.builder()
            .id(projectRoleId)
            .roleId(roleId)
            .build());
        log.info("Project Role has been modified ID={}", projectRoleId);
        return res > 0;
    }

    public Boolean deleteProjectRole(String projectUrl, Long projectRoleId) {
        int res = projectRoleMapper.deleteProjectRole(projectRoleId);
        log.info("Project Role has been deleted ID={}", projectRoleId);
        return res > 0;
    }

}
