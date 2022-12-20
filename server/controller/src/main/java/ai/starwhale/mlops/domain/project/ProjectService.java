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

import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.project.StatisticsVo;
import ai.starwhale.mlops.api.protocol.user.ProjectRoleVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectRoleMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCounts;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
public class ProjectService {

    public static final String PROJECT_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{2,80}$";

    private final ProjectMapper projectMapper;

    private final ProjectManager projectManager;

    private final ProjectRoleMapper projectRoleMapper;

    private final IdConverter idConvertor;

    private final UserService userService;

    private static final String DELETE_SUFFIX = ".deleted";

    public ProjectService(ProjectMapper projectMapper,
            ProjectManager projectManager,
            ProjectRoleMapper projectRoleMapper,
            IdConverter idConvertor,
            UserService userService) {
        this.projectMapper = projectMapper;
        this.projectManager = projectManager;
        this.projectRoleMapper = projectRoleMapper;
        this.idConvertor = idConvertor;
        this.userService = userService;
    }

    /**
     * Find a project by parameters.
     *
     * @param projectUrl Project URL must be set.
     * @return Optional of a ProjectVo object.
     */
    public ProjectVo findProject(String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectEntity projectEntity = projectMapper.find(projectId);
        if (projectEntity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT, "Unable to find project"),
                    HttpStatus.BAD_REQUEST);
        }
        return ProjectVo.fromEntity(projectEntity, idConvertor,
                userService.findUserById(projectEntity.getOwnerId()));
    }

    /**
     * Get the list of projects.
     *
     * @param projectName Search by project name prefix if the project name is set.
     * @param pageParams  Paging parameters.
     * @return A list of ProjectVo objects
     */
    public PageInfo<ProjectVo> listProject(String projectName, PageParams pageParams, OrderParams orderParams,
            User user) {
        Long userId = user.getId();
        List<Role> sysRoles = userService.getProjectRolesOfUser(user, "0");
        for (Role sysRole : sysRoles) {
            if (sysRole.getAuthority().equals("OWNER")) {
                userId = null;
                break;
            }
        }

        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<ProjectEntity> entities = projectManager.listProjects(projectName, userId, orderParams);
        List<Long> ids = entities.stream().map(ProjectEntity::getId).collect(Collectors.toList());
        Map<Long, ProjectObjectCounts> countMap = projectManager.getObjectCountsOfProjects(
                ids);

        return PageUtil.toPageInfo(entities, entity -> {
            ProjectVo vo = ProjectVo.fromEntity(entity, idConvertor,
                    userService.findUserById(entity.getOwnerId()));
            ProjectObjectCounts count = countMap.get(entity.getId());
            if (count != null) {
                vo.setStatistics(StatisticsVo.builder()
                        .modelCounts(count.getCountModel())
                        .datasetCounts(count.getCountDataset())
                        .runtimeCounts(count.getCountRuntime())
                        .memberCounts(count.getCountMember())
                        .evaluationCounts(count.getCountJob())
                        .build());
            }
            return vo;
        });
    }

    /**
     * Create a new project
     *
     * @param project Object of the project to create.
     * @return ID of the project was created.
     */
    @Transactional
    public Long createProject(Project project) {
        Assert.notNull(project.getName(), "Project name must not be null");
        if (projectManager.existProject(project.getName(), project.getOwner().getId())) {
            //项目存在且未被删除
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT,
                            String.format("Project %s already exists", project.getName())),
                    HttpStatus.BAD_REQUEST);
        }

        ProjectEntity entity = ProjectEntity.builder()
                .projectName(project.getName())
                .ownerId(project.getOwner().getId())
                .privacy(project.getPrivacy().getValue())
                .projectDescription(project.getDescription())
                .isDefault(project.isDefault() ? 1 : 0)
                .build();
        projectMapper.insert(entity);
        projectRoleMapper.insertByRoleName(entity.getOwnerId(), entity.getId(), "owner");
        log.info("Project has been created. ID={}, NAME={}", entity.getId(), entity.getProjectName());
        return entity.getId();
    }

    /**
     * Delete a project
     *
     * @param projectUrl Project URL must be set.
     * @return Is the operation successful.
     */
    @Transactional
    public Boolean deleteProject(String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectEntity entity = projectMapper.find(projectId);
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.PROJECT, "Unable to find project");
        }
        if (entity.getIsDefault() > 0) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT, "Default project cannot be deleted."),
                    HttpStatus.BAD_REQUEST);
        }
        entity.setProjectName(entity.getProjectName() + DELETE_SUFFIX + "." + entity.getId());
        projectMapper.update(entity);
        int res = projectMapper.remove(entity.getId());
        log.info("Project has been deleted. ID={}", entity.getId());
        return res > 0;
    }

    @Transactional
    public Long recoverProject(String projectUrl) {
        String projectName = null;
        Long ownerId = null;
        Long id;
        if (idConvertor.isId(projectUrl)) {
            id = idConvertor.revert(projectUrl);
            ProjectEntity entity = projectMapper.find(id);
            if (entity == null) {
                throw new SwNotFoundException(ResourceType.PROJECT,
                        "Recover project error. Project can not be found.");
            }
            projectName = entity.getProjectName().substring(0,
                    entity.getProjectName().lastIndexOf(DELETE_SUFFIX));
            ownerId = entity.getOwnerId();
            entity.setProjectName(projectName);
        } else {
            String[] arr = projectManager.splitProjectUrl(projectUrl);
            if (arr.length > 1) {
                projectName = arr[1];
                if (idConvertor.isId(arr[0])) {
                    ownerId = idConvertor.revert(arr[0]);
                } else {
                    ownerId = Optional.of(userService.loadUserByUsername(arr[0]))
                            .orElseThrow(() -> new SwNotFoundException(ResourceType.USER,
                                    "Recover project error. User can not be found. "))
                            .getId();
                }
            } else {
                projectName = projectUrl;
                ownerId = userService.currentUserDetail().getId();
            }

            // To restore projects by name, need to check whether there are duplicate names
            List<ProjectEntity> deletedProjects = projectMapper.listRemovedProjects(projectName + DELETE_SUFFIX,
                    ownerId);
            if (deletedProjects.size() > 1) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.PROJECT,
                                StrUtil.format("Recover project error. Duplicate names [%s] of deleted project. ",
                                        projectName)),
                        HttpStatus.BAD_REQUEST);
            } else if (deletedProjects.size() == 0) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.PROJECT,
                                StrUtil.format("Recover project error. Can not find deleted project [%s].",
                                        projectName)),
                        HttpStatus.BAD_REQUEST);
            }
            id = deletedProjects.get(0).getId();
        }

        // Check for duplicate names
        if (projectManager.existProject(projectName, ownerId)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT,
                            String.format("Recover project error. Project %s already exists", projectName)),
                    HttpStatus.BAD_REQUEST);
        }
        projectMapper.update(ProjectEntity.builder()
                .id(id)
                .projectName(projectName)
                .build());
        projectMapper.recover(id);
        log.info("Project has been recovered. Name={}", projectName);
        return id;
    }

    @Transactional
    public Boolean modifyProject(String projectUrl, String projectName, String description, Long userId,
            String privacy) {
        Long projectId = projectManager.getProjectId(projectUrl);
        if (StrUtil.isNotEmpty(projectName)) {
            ProjectEntity existProject = projectMapper.findByNameForUpdateAndOwner(projectName, userId);
            if (existProject != null && !Objects.equals(existProject.getId(), projectId)) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.PROJECT,
                                String.format("Project %s already exists", projectName)),
                        HttpStatus.BAD_REQUEST);
            }
        }
        ProjectEntity entity = ProjectEntity.builder()
                .id(projectId)
                .projectName(projectName)
                .projectDescription(description)
                .ownerId(userId)
                .privacy(privacy == null ? null : Privacy.fromName(privacy).getValue())
                .build();
        int res = projectMapper.update(entity);
        log.info("Project has been modified ID={}", entity.getId());
        return res > 0;
    }

    public List<ProjectRoleVo> listProjectRoles(String projectUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        List<ProjectRoleEntity> entities = projectRoleMapper.listByProject(projectId);
        return entities.stream()
                .map(entity -> {
                    ProjectRoleVo vo = ProjectRoleVo.builder()
                            .id(idConvertor.convert(entity.getId()))
                            .role(userService.findRoleById(entity.getRoleId()))
                            .user(userService.findUserById(entity.getUserId()))
                            .project(findProject(projectUrl))
                            .build();
                    return vo;
                }).collect(Collectors.toList());
    }

    public Boolean addProjectRole(String projectUrl, Long userId, Long roleId) {
        Long projectId = projectManager.getProjectId(projectUrl);
        ProjectRoleEntity entity = ProjectRoleEntity.builder()
                .userId(userId)
                .roleId(roleId)
                .projectId(projectId)
                .build();
        int res = projectRoleMapper.insert(entity);
        log.info("Project Role has been created ID={}", entity.getId());
        return res > 0;
    }

    public Boolean modifyProjectRole(String projectUrl, Long projectRoleId, Long roleId) {
        int res = projectRoleMapper.updateRole(projectRoleId, roleId);
        log.info("Project Role has been modified ID={}", projectRoleId);
        return res > 0;
    }

    public Boolean deleteProjectRole(String projectUrl, Long projectRoleId) {
        int res = projectRoleMapper.delete(projectRoleId);
        log.info("Project Role has been deleted ID={}", projectRoleId);
        return res > 0;
    }

}
